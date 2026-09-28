package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.KeywordActionReplacements
import com.wingedsheep.engine.handlers.effects.ReplaceableKeywordAction
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EmitExploredEventEffect
import com.wingedsheep.sdk.scripting.effects.ExploreEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for ExploreEffect.
 *
 * "Reveal the top card of your library. If it's a land card, put it into your hand.
 * Otherwise, put a +1/+1 counter on this creature, then put the card back on top of your
 * library or put it into your graveyard."
 *
 * The exploring player is the effect controller. The exploring creature is [ExploreEffect.target].
 *
 * Before the explore proper, applicable [com.wingedsheep.sdk.scripting.ModifyKeywordAction]
 * replacements (CR 614) on the battlefield are consulted via [KeywordActionReplacements] — like
 * [com.wingedsheep.sdk.scripting.ReplaceDrawWith], explore isn't a generic replaceable
 * event, so it's checked here directly. A match re-issues the explore as
 * `Composite(prefixEffect, ExploreEffect(sameCreature, replacementsApplied = true))` through the
 * registry [recurse] runner, reusing the composite executor's pause-sequencing (a Scry prefix
 * finishes its top/bottom decision before the explore runs).
 *
 * @param recurse registry entry point for delegating the Composite (handed to [PermanentExecutors]
 *   by the registry at construction).
 */
class ExploreEffectExecutor(
    private val zones: ZoneTransitionService,
    private val recurse: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<ExploreEffect> {

    override val effectType: KClass<ExploreEffect> = ExploreEffect::class

    override fun execute(
        state: GameState,
        effect: ExploreEffect,
        context: EffectContext
    ): EffectResult {
        val exploringCreatureId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val explorerId = context.controllerId

        // CR 614: apply explore-modifying replacements before the explore proper, unless this is
        // the post-replacement re-issue (replacementsApplied). The collected prefixes run as a
        // Composite ahead of the guarded explore so a pausing prefix (Scry) sequences correctly.
        if (!effect.replacementsApplied) {
            val prefixEffects = KeywordActionReplacements.collectPrefixes(
                state, exploringCreatureId, ReplaceableKeywordAction.EXPLORE,
                predicateEvaluator = zones.predicateEvaluator
            )
            if (prefixEffects.isNotEmpty()) {
                val composite = CompositeEffect(
                    prefixEffects + ExploreEffect(
                        target = EffectTarget.SpecificEntity(exploringCreatureId),
                        replacementsApplied = true
                    )
                )
                return recurse(state, composite, context)
            }
        }
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // CR 701.44b: the permanent "explores" even if the reveal was impossible (empty library).
        // Emit the explored event (revealedCardWasLand = null) so "whenever a creature you control
        // explores" (ANY) still fires; the land/nonland reveal-type triggers don't match.
        fun exploredEvent(wasLand: Boolean?) = PermanentExploredEvent(
            exploringPermanentId = exploringCreatureId,
            controllerId = explorerId,
            revealedCardWasLand = wasLand,
            sourceName = sourceName
        )

        val library = state.getLibrary(explorerId)
        if (library.isEmpty()) {
            return EffectResult.success(state, listOf(exploredEvent(null)))
        }

        val topCardId = library.first()
        val topCardContainer = state.getEntity(topCardId)
            ?: return EffectResult.success(state)
        val topCardComponent = topCardContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        val topCardName = topCardComponent.name
        val topCardImageUri = topCardComponent.imageUri

        val revealEvent = CardsRevealedEvent(
            revealingPlayerId = explorerId,
            cardIds = listOf(topCardId),
            cardNames = listOf(topCardName),
            imageUris = listOf(topCardImageUri),
            source = sourceName
        )

        return if (topCardComponent.typeLine.isLand) {
            // Land: move directly to hand
            val transition = zones.moveToZone(state, topCardId, Zone.HAND)
            EffectResult.success(
                transition.state,
                listOf(revealEvent) + transition.events + exploredEvent(true)
            )
        } else {
            // Non-land: mark card revealed to all players, add +1/+1 counter, then ask:
            // back to top of library (stays visible) or graveyard?
            val revealedComponent = state.turnOrder.fold(RevealedToComponent(emptySet())) { acc, pid ->
                acc.withPlayer(pid)
            }
            val stateWithRevealed = state.updateEntity(topCardId) { it.with(revealedComponent) }

            val (stateAfterCounter, counterEvents) = addPlusOneCounter(stateWithRevealed, exploringCreatureId, context)

            val decision = { decisionId: String -> YesNoDecision(
                id = decisionId,
                playerId = explorerId,
                prompt = "Put $topCardName back on top of your library? (No = graveyard)",
                context = DecisionContext(
                    sourceId = context.sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                yesText = "Library (top)",
                noText = "Graveyard"
            ) }

            // Defer the explored event to after the top/graveyard move resolves (CR 701.44b): a
            // game event emitted in the paused batch below does not reliably fire watcher triggers,
            // so both continuation branches end by emitting it from a completed resolution.
            val emitExplored = EmitExploredEventEffect(
                target = EffectTarget.SpecificEntity(exploringCreatureId),
                revealedCardWasLand = false
            )
            val continuation = MayAbilityContinuation(
                playerId = explorerId,
                sourceName = sourceName,
                effectIfYes = CompositeEffect(listOf(
                    MoveToZoneEffect(
                        target = EffectTarget.SpecificEntity(topCardId),
                        destination = Zone.LIBRARY,
                        placement = ZonePlacement.Top
                    ),
                    emitExplored
                )),
                effectIfNo = CompositeEffect(listOf(
                    MoveToZoneEffect(
                        target = EffectTarget.SpecificEntity(topCardId),
                        destination = Zone.GRAVEYARD
                    ),
                    emitExplored
                )),
                effectContext = context
            )

            EffectResult.from(stateAfterCounter.suspendForDecision(decision, continuation, listOf(revealEvent) + counterEvents))
        }
    }

    private fun addPlusOneCounter(
        state: GameState,
        creatureId: EntityId,
        context: EffectContext
    ): Pair<GameState, List<GameEvent>> {
        if (!state.projectedState.canReceiveCounters(creatureId)) {
            return state to emptyList()
        }
        val current = state.getEntity(creatureId)?.get<CountersComponent>() ?: CountersComponent()
        val count = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state, creatureId, CounterType.PLUS_ONE_PLUS_ONE, 1, placerId = context.controllerId,
            predicateEvaluator = zones.predicateEvaluator
        )
        val updated = state.updateEntity(creatureId) {
            it.with(current.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        }
        // Record the kind and the placer, not just a bare marker: the exploring player is the one
        // putting the counter on, so "each creature you control that you've put one or more +1/+1
        // counters on this turn" (Kid Loki) covers a creature you made explore.
        val (newState, firstThisTurn, firstOfTypeThisTurn) =
            com.wingedsheep.engine.handlers.effects.DamageUtils.recordCounterPlacement(
                updated,
                creatureId,
                CounterType.PLUS_ONE_PLUS_ONE,
                placerId = context.controllerId,
            )
        val name = state.getEntity(creatureId)?.get<CardComponent>()?.name ?: ""
        return newState to listOf(
            CountersAddedEvent(
                creatureId,
                CounterType.PLUS_ONE_PLUS_ONE,
                count,
                name,
                firstThisTurn, firstOfTypeThisTurn = firstOfTypeThisTurn,
                placedBy = context.controllerId,
            )
        )
    }
}
