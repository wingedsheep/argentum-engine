package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ForEachContinuation
import com.wingedsheep.engine.core.ForEachItem
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.reflect.KClass

/**
 * The single executor behind [ForEachEffect]: enumerate the iteration space once
 * (snapshot semantics — entities destroyed or added mid-iteration don't change the
 * list), then run the body once per item with the per-space context binding.
 *
 * Pause safety is uniform across all spaces: before each iteration with work left
 * after it, a [ForEachContinuation] carrying the remaining items is pre-pushed. If the
 * body pauses for a decision, that frame sits beneath the body's own frames and the
 * resumer continues the loop via [processItems]; if the body completes synchronously,
 * the frame is popped again. (Pre-unification, only the target/player loops did this —
 * a pausing body inside a group/collection/color loop silently dropped the remaining
 * iterations.)
 *
 * Per-space context binding (see [IterationSpace] for the contract):
 * - Targets: the current target becomes the context's only target (`ContextTarget(0)`),
 *   storedCollections wiped.
 * - Players: `controllerId` rebound to the current player
 *   relative to them, storedCollections wiped.
 * - Collection / Group: `pipeline.iterationTarget` set so `EffectTarget.Self` resolves
 *   to the current entity; outer collections preserved.
 * - ColorsOf: `chosenColor` set — the same channel `ChooseColorThen` feeds.
 */
class ForEachExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<ForEachEffect> {

    override val effectType: KClass<ForEachEffect> = ForEachEffect::class

    override fun execute(
        state: GameState,
        effect: ForEachEffect,
        context: EffectContext
    ): EffectResult {
        val items = enumerateItems(state, effect.space, context)
        if (items.isEmpty()) {
            return EffectResult.success(state)
        }

        // Group with noRegenerate: mark every matched entity before any iteration
        // applies, so the first iteration's destruction can't be regenerated either.
        var currentState = state
        val space = effect.space
        if (space is IterationSpace.Group && space.noRegenerate) {
            for (item in items) {
                if (item is ForEachItem.OfEntity) {
                    currentState = addCantBeRegenerated(currentState, item.entityId, context)
                }
            }
        }

        return processItems(currentState, effect, items, context)
    }

    /**
     * Run the body for each item, pre-pushing a [ForEachContinuation] with the items
     * still to go. Called from [execute] and from the continuation resumer.
     */
    fun processItems(
        state: GameState,
        effect: ForEachEffect,
        items: List<ForEachItem>,
        outerContext: EffectContext
    ): EffectResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, item) in items.withIndex()) {
            val remainingItems = items.drop(index + 1)

            val iterationContext = bindIterationContext(currentState, outerContext, item)

            val stateForExecution = if (remainingItems.isNotEmpty()) {
                currentState.pushContinuation(
                    ForEachContinuation(
                        decisionId = "pending",
                        remainingItems = remainingItems,
                        effect = effect,
                        effectContext = outerContext
                    )
                )
            } else {
                currentState
            }

            val result = effectExecutor(stateForExecution, effect.body, iterationContext)

            if (result.isPaused) {
                // The body needs a decision; our ForEachContinuation is beneath its
                // frames and resumes the remaining items once the body completes.
                return EffectResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            // Pop the pre-pushed continuation (it wasn't needed). A failed body
            // (per CR 608.2 partial resolution) still continues with the next item.
            currentState = if (remainingItems.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)
        }

        return EffectResult.success(currentState, allEvents)
    }

    /** Snapshot the iteration space into concrete items. */
    private fun enumerateItems(
        state: GameState,
        space: IterationSpace,
        context: EffectContext
    ): List<ForEachItem> = when (space) {
        is IterationSpace.Targets ->
            context.targets.map { ForEachItem.OfTarget(it) }

        is IterationSpace.Players ->
            resolvePlayers(space.players, state, context).map { ForEachItem.OfPlayer(it) }

        is IterationSpace.Collection ->
            context.pipeline.storedCollections[space.collection].orEmpty()
                .map { ForEachItem.OfEntity(it) }

        is IterationSpace.Group ->
            resolveGroup(state, space, context).map { ForEachItem.OfEntity(it) }

        is IterationSpace.ColorsOf -> {
            val sourceId = TargetResolutionUtils.resolveEntityReference(space.source, context, state)
            if (sourceId == null) {
                emptyList()
            } else {
                val colorNames = readSourceColors(state, sourceId)
                // Canonical WUBRG order for deterministic event sequencing.
                Color.entries.filter { it.name in colorNames }.map { ForEachItem.OfColor(it) }
            }
        }
    }

    /**
     * Bind the per-iteration execution context for the space's contract. The item kind
     * always matches the space by construction ([enumerateItems]).
     */
    private fun bindIterationContext(
        state: GameState,
        outerContext: EffectContext,
        item: ForEachItem
    ): EffectContext = when (item) {
        is ForEachItem.OfTarget -> outerContext.copy(
            targets = listOf(item.target),
            pipeline = outerContext.pipeline.copy(storedCollections = emptyMap())
        )

        // Rebind the controller to the iterated player so You / Chooser.Controller /
        // Chooser.Opponent inside the loop resolve relative to *this* player (e.g.
        // Bend or Break: an opponent of each separating player chooses that player's
        // pile), not the original caster.
        is ForEachItem.OfPlayer -> outerContext.copy(
            controllerId = item.playerId,
            pipeline = outerContext.pipeline.copy(storedCollections = emptyMap())
        )

        is ForEachItem.OfEntity -> outerContext.copy(
            pipeline = outerContext.pipeline.copy(iterationTarget = item.entityId)
        )

        is ForEachItem.OfColor -> outerContext.copy(chosenColor = item.color)
    }

    private fun resolvePlayers(player: Player, state: GameState, context: EffectContext): List<EntityId> {
        return when (player) {
            Player.Each -> state.activePlayers
            Player.ActivePlayerFirst -> {
                val activePlayer = state.activePlayerId ?: return state.activePlayers
                listOf(activePlayer) + state.activePlayers.filter { it != activePlayer }
            }
            Player.You -> listOf(context.controllerId)
            Player.EachOpponent -> state.getOpponents(context.controllerId)
            Player.TargetOpponent, Player.TargetPlayer -> listOfNotNull(
                TargetResolutionUtils.resolvePlayerRef(player, context, state)
            )
            else -> state.activePlayers
        }
    }

    /**
     * Resolve a group filter to the battlefield entities it matches, honoring a
     * chosen-subtype key and the filter's excludeSelf flag.
     */
    private fun resolveGroup(
        state: GameState,
        space: IterationSpace.Group,
        context: EffectContext
    ): List<EntityId> {
        val filter = space.filter

        // If the filter references a chosen subtype, resolve it from context
        val chosenSubtype = filter.chosenSubtypeKey?.let { key ->
            context.pipeline.chosenValues[key]
        }
        // If a chosen subtype key is specified but no value was chosen, return empty
        if (filter.chosenSubtypeKey != null && chosenSubtype == null) {
            return emptyList()
        }

        val excludeSelfId = if (filter.excludeSelf) context.sourceId else null
        val matched = BattlefieldFilterUtils.findMatchingOnBattlefield(state, filter.baseFilter, context, excludeSelfId)

        // Additionally filter by chosen subtype if specified
        return if (chosenSubtype != null) {
            val projected = state.projectedState
            matched.filter { projected.hasSubtype(it, chosenSubtype) }
        } else {
            matched
        }
    }

    /**
     * Colors of the entity for [IterationSpace.ColorsOf]: projection is authoritative
     * on the battlefield (Devoid / Layer-5 changes honored, including an authoritative
     * empty set); off battlefield, printed colors as last-known information.
     */
    private fun readSourceColors(state: GameState, entityId: EntityId): Set<String> {
        if (state.getBattlefield().contains(entityId)) {
            return state.projectedState.getColors(entityId)
        }
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return emptySet()
        return card.colors.map { it.name }.toSet()
    }

    private fun addCantBeRegenerated(
        state: GameState,
        entityId: EntityId,
        context: EffectContext
    ): GameState {
        return state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.CantBeRegenerated,
            affectedEntities = setOf(entityId),
            duration = Duration.EndOfTurn,
            context = context
        )
    }
}
