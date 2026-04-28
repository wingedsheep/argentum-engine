package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DrawReplacementActivationContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.StaticDrawReplacementContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.PreventDraw
import com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import java.util.UUID

/**
 * Runs the three draw-replacement checks that fire before each individual card
 * is drawn, in the order required by the rules:
 *
 *  1. Unified draw replacement shield
 *     ([com.wingedsheep.engine.mechanics.layers.SerializableModification.ReplaceDrawWithEffect])
 *     — Words of Worship / Wind / War / Waste / Wilding and friends, dispatched
 *     via [DrawReplacementShieldConsumer].
 *  2. Static draw prevention effects — Mornsong Aria / Narset-style effects.
 *  3. Static draw replacement effect — Parallel Thoughts-style optional
 *     yes/no prompt that replaces the draw with an alternative effect.
 *  4. Prompt-on-draw activated ability — `activatedAbilities` with
 *     `promptOnDraw = true`, e.g. Words of Wind cycling.
 *
 * The dispatcher is the single place both [DrawCardsExecutor] (spell/ability
 * draws) and [com.wingedsheep.engine.core.DrawPhaseManager] (draw-step draws)
 * call into to ask "does anything intercept this draw?" — unifying the two
 * copies of this logic that previously existed in those two classes.
 */
class DrawReplacementDispatcher(
    private val cardRegistry: CardRegistry,
    private val effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)?
) {
    /**
     * The outcome of [checkBeforeDraw].
     */
    sealed interface DispatchResult {
        /** No replacement fires — caller should perform a primitive draw. */
        data object None : DispatchResult

        /**
         * A replacement completed synchronously. The caller should **not**
         * perform a primitive draw this iteration; it should fold [state] and
         * [events] into its running state and proceed to the next iteration.
         */
        data class Replaced(val state: GameState, val events: List<GameEvent>) : DispatchResult

        /**
         * A replacement emitted a decision and the draw is paused. The caller
         * must return this result (possibly with its own events prepended).
         */
        data class Paused(val result: EffectResult) : DispatchResult
    }

    /**
     * Run the three checks in order. Returns as soon as one fires.
     *
     * @param drawsLeftIncludingThis the number of draws remaining including the
     *     current one (i.e., `count - i` in an outer `for (i in 0 until count)`
     *     loop).
     * @param drawnCardsSoFar cards already drawn before this iteration, used
     *     for continuation state and partial-draw event flushing.
     * @param isDrawStep whether this is the active player's draw-step draw
     *     (vs a spell/ability draw).
     * @param skipStaticReplacement skip the Parallel Thoughts check; set by
     *     callers that pass the historical `skipPrompts = true` flag when
     *     resuming after a prior decision already handled replacements.
     * @param skipPromptOnDraw skip the prompt-on-draw check; set by the
     *     draw-step path (which asks up-front once in `performDrawStep`) and
     *     by resume paths that have already handled the prompt.
     * @param declinedSourceIds prompt-on-draw sources the player has already
     *     declined in this decision chain — re-declining the same source would
     *     loop forever.
     */
    fun checkBeforeDraw(
        state: GameState,
        playerId: EntityId,
        drawsLeftIncludingThis: Int,
        drawnCardsSoFar: List<EntityId>,
        isDrawStep: Boolean,
        skipStaticReplacement: Boolean = false,
        skipPromptOnDraw: Boolean = false,
        declinedSourceIds: List<EntityId> = emptyList()
    ): DispatchResult {
        // 1. Unified draw replacement shield.
        val shieldConsumer = effectExecutor?.let { DrawReplacementShieldConsumer(it) }
        if (shieldConsumer != null) {
            val shieldResult = shieldConsumer.consumeShield(
                state = state,
                playerId = playerId,
                remainingDraws = drawsLeftIncludingThis - 1,
                drawnCardsSoFar = drawnCardsSoFar,
                eventsSoFar = emptyList(),
                isDrawStep = isDrawStep
            )
            if (shieldResult != null) {
                return when (shieldResult) {
                    is DrawReplacementShieldConsumer.ConsumeResult.Paused ->
                        DispatchResult.Paused(shieldResult.result)
                    is DrawReplacementShieldConsumer.ConsumeResult.Synchronous ->
                        DispatchResult.Replaced(shieldResult.state, shieldResult.events)
                }
            }
        }

        // 2. Mandatory static draw prevention.
        if (isDrawPrevented(state, playerId)) {
            return DispatchResult.Replaced(state, emptyList())
        }

        // 3. Static draw replacement (Parallel Thoughts).
        if (!skipStaticReplacement) {
            val staticResult = checkStaticDrawReplacement(
                state, playerId, drawsLeftIncludingThis, drawnCardsSoFar, isDrawStep
            )
            if (staticResult != null) {
                return DispatchResult.Paused(staticResult)
            }
        }

        // 4. Prompt-on-draw activated ability (Words of Wind).
        if (!skipPromptOnDraw) {
            val promptResult = checkPromptOnDraw(
                state, playerId, drawsLeftIncludingThis, drawnCardsSoFar, isDrawStep, declinedSourceIds
            )
            if (promptResult != null) {
                return DispatchResult.Paused(promptResult)
            }
        }

        return DispatchResult.None
    }

    private fun isDrawPrevented(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementSource = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (effect in replacementSource.replacementEffects) {
                if (effect !is PreventDraw) continue

                val drawEvent = effect.appliesTo
                if (drawEvent !is com.wingedsheep.sdk.scripting.GameEvent.DrawEvent) continue

                val sourceControllerId = container.get<ControllerComponent>()?.playerId
                when (drawEvent.player) {
                    Player.Each -> return true
                    Player.You -> if (playerId == sourceControllerId) return true
                    Player.Opponent, Player.EachOpponent -> if (sourceControllerId != null && playerId != sourceControllerId) return true
                    else -> {}
                }
            }
        }
        return false
    }

    /**
     * Check if [playerId] controls a permanent with an optional static draw
     * replacement effect (e.g., Parallel Thoughts). If so, returns a paused
     * [ExecutionResult] with a yes/no decision; otherwise returns `null`.
     *
     * This is exposed as part of the dispatcher's internal API (rather than
     * strictly private) so that the draw-step resume path can ask the question
     * in isolation without running the whole dispatch pipeline.
     */
    fun checkStaticDrawReplacement(
        state: GameState,
        playerId: EntityId,
        drawCount: Int,
        drawnCardsSoFar: List<EntityId>,
        isDrawStep: Boolean
    ): EffectResult? {
        val projected = state.projectedState
        val controlledPermanents = projected.getBattlefieldControlledBy(playerId)

        for (permanentId in controlledPermanents) {
            val container = state.getEntity(permanentId) ?: continue
            val replacementSource = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (re in replacementSource.replacementEffects) {
                if (re !is ReplaceDrawWithEffect) continue
                if (!re.optional) continue

                val card = container.get<CardComponent>() ?: continue
                val linkedExile = container.get<LinkedExileComponent>()
                val pileCount = linkedExile?.exiledIds?.size ?: 0

                val decisionId = UUID.randomUUID().toString()
                val prompt = "Use ${card.name}? Put the top card of the exiled pile " +
                    "($pileCount cards remaining) into your hand instead of drawing?"

                val decision = YesNoDecision(
                    id = decisionId,
                    playerId = playerId,
                    prompt = prompt,
                    context = DecisionContext(
                        sourceId = permanentId,
                        sourceName = card.name,
                        phase = DecisionPhase.RESOLUTION
                    )
                )

                val continuation = StaticDrawReplacementContinuation(
                    decisionId = decisionId,
                    drawingPlayerId = playerId,
                    sourceId = permanentId,
                    sourceName = card.name,
                    replacementEffect = re.replacementEffect,
                    drawCount = drawCount,
                    isDrawStep = isDrawStep,
                    drawnCardsSoFar = drawnCardsSoFar
                )

                val stateWithDecision = state.withPendingDecision(decision)
                val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

                return EffectResult.paused(
                    stateWithContinuation,
                    decision,
                    listOf(
                        DecisionRequestedEvent(
                            decisionId = decisionId,
                            playerId = playerId,
                            decisionType = "YES_NO",
                            prompt = decision.prompt
                        )
                    )
                )
            }
        }
        return null
    }

    /**
     * Check if [playerId] has a "prompt on draw" activated ability they can
     * afford (e.g., Words of Wind). If so, returns a paused [ExecutionResult]
     * with a mana-source selection decision; otherwise returns `null`.
     *
     * [declinedSourceIds] carries sources the player has already declined in
     * this decision chain — without it, declining a prompt would loop forever
     * as the dispatcher immediately re-offered the same ability.
     */
    fun checkPromptOnDraw(
        state: GameState,
        playerId: EntityId,
        drawCount: Int,
        drawnCardsSoFar: List<EntityId>,
        isDrawStep: Boolean,
        declinedSourceIds: List<EntityId> = emptyList()
    ): EffectResult? {
        val projected = state.projectedState
        val controlledPermanents = projected.getBattlefieldControlledBy(playerId)

        for (permanentId in controlledPermanents) {
            if (permanentId in declinedSourceIds) continue
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (ability in cardDef.script.activatedAbilities) {
                if (!ability.promptOnDraw) continue

                val manaCost = when (val cost = ability.cost) {
                    is AbilityCost.Mana -> cost.cost
                    is AbilityCost.Composite ->
                        cost.costs.filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost
                    else -> null
                } ?: continue

                val manaSolver = ManaSolver(cardRegistry)
                if (!manaSolver.canPay(state, playerId, manaCost)) continue

                val sources = manaSolver.findAvailableManaSources(state, playerId)
                val sourceOptions = sources.map { source ->
                    ManaSourceOption(
                        entityId = source.entityId,
                        name = source.name,
                        producesColors = source.producesColors,
                        producesColorless = source.producesColorless
                    )
                }

                val solution = manaSolver.solve(state, playerId, manaCost)
                val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

                val decisionId = UUID.randomUUID().toString()
                val decision = SelectManaSourcesDecision(
                    id = decisionId,
                    playerId = playerId,
                    prompt = "Pay ${manaCost} to activate ${card.name}?",
                    context = DecisionContext(
                        sourceId = permanentId,
                        sourceName = card.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    availableSources = sourceOptions,
                    requiredCost = manaCost.toString(),
                    autoPaySuggestion = autoPaySuggestion,
                    canDecline = true
                )

                val continuation = DrawReplacementActivationContinuation(
                    decisionId = decisionId,
                    drawingPlayerId = playerId,
                    sourceId = permanentId,
                    sourceName = card.name,
                    abilityEffect = ability.effect,
                    manaCost = manaCost.toString(),
                    drawCount = drawCount,
                    isDrawStep = isDrawStep,
                    drawnCardsSoFar = drawnCardsSoFar,
                    targetRequirements = ability.targetRequirements,
                    declinedSourceIds = declinedSourceIds
                )

                val stateWithDecision = state.withPendingDecision(decision)
                val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

                return EffectResult.paused(
                    stateWithContinuation,
                    decision,
                    listOf(
                        DecisionRequestedEvent(
                            decisionId = decisionId,
                            playerId = playerId,
                            decisionType = "SELECT_MANA_SOURCES",
                            prompt = decision.prompt
                        )
                    )
                )
            }
        }
        return null
    }
}
