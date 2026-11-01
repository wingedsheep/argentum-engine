package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.CardRevealedForMiracleEvent
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.MiracleRevealContinuation
import com.wingedsheep.engine.core.OptionalFirstDrawRevealContinuation
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardPrimitive
import com.wingedsheep.engine.handlers.effects.drawing.DrawLoop
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.StaticDrawReplacementContinuation
import com.wingedsheep.engine.core.DrawReplacementRemainingDrawsContinuation
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.core.Outcome

class DrawReplacementContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(StaticDrawReplacementContinuation::class, ::resumeStaticDrawReplacement),
        resumer(MiracleRevealContinuation::class, ::resumeMiracleReveal),
        resumer(OptionalFirstDrawRevealContinuation::class, ::resumeOptionalFirstDrawReveal)
    )

    /**
     * Resume after the player answers an optional first-draw reveal ("you may reveal the first card
     * you draw each turn as you draw it").
     *
     * Yes, with the card still in hand: emit [com.wingedsheep.engine.core.CardRevealedFromDrawEvent]
     * naming only this source, so only its "whenever you reveal … this way" trigger fires (CR 607.1).
     * No: nothing happens. Then the next "as you draw it" question (another source, the miracle
     * reveal) is asked, or the rest of the draw instruction continues from the frame beneath.
     */
    fun resumeOptionalFirstDrawReveal(
        state: GameState,
        continuation: OptionalFirstDrawRevealContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for the first-draw reveal")
        }
        val inHand = continuation.cardId in
            state.getZone(com.wingedsheep.engine.state.ZoneKey(continuation.playerId, com.wingedsheep.sdk.core.Zone.HAND))
        if (!inHand) return checkForMore(state, emptyList())
        val events = if (!response.choice) emptyList() else listOfNotNull(
            DrawCardPrimitive.firstDrawRevealEvent(
                state, continuation.playerId, continuation.cardId, listOf(continuation.sourceId)
            )
        )
        return DrawLoop.askFirstDrawQuestion(
            state, continuation.playerId, continuation.cardId,
            continuation.laterSources, continuation.miracleCost, events
        ) ?: checkForMore(state, events)
    }

    /**
     * Resume after the player answers the miracle reveal question (CR 702.94a).
     *
     * Yes: reveal the card to the table and emit [CardRevealedForMiracleEvent], which the trigger
     * detector turns into the miracle trigger. No: nothing happens and nothing is shown — the
     * question was private, so declining leaves no trace. Either way the rest of the draw
     * instruction continues from the frame waiting beneath.
     */
    fun resumeMiracleReveal(
        state: GameState,
        continuation: MiracleRevealContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for the miracle reveal")
        }
        val inHand = continuation.cardId in
            state.getZone(com.wingedsheep.engine.state.ZoneKey(continuation.playerId, com.wingedsheep.sdk.core.Zone.HAND))
        if (!response.choice || !inHand) return checkForMore(state, emptyList())

        val (revealed, revealEvent) = com.wingedsheep.engine.handlers.effects.composite.MayRevealCardFromHandEffectExecutor
            .emitReveal(state, continuation.playerId, continuation.cardId, "Miracle")
        val miracleEvent = CardRevealedForMiracleEvent(
            playerId = continuation.playerId,
            cardId = continuation.cardId,
            cardName = revealEvent.cardNames.firstOrNull() ?: "card",
            cost = continuation.cost
        )
        return checkForMore(revealed, listOf(revealEvent, miracleEvent))
    }

    /**
     * Resume after the player answers yes/no for an optional static draw replacement effect
     * (e.g., Parallel Thoughts).
     *
     * Yes: execute the replacement effect, then draw remaining (drawCount - 1).
     * No: draw 1 normally, then draw remaining (drawCount - 1).
     */
    fun resumeStaticDrawReplacement(
        state: GameState,
        continuation: StaticDrawReplacementContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for static draw replacement")
        }

        val playerId = continuation.drawingPlayerId
        // Keep the rest of the instruction below any decisions the replacement creates.
        // Even an empty tail provides the boundary that clears the current draw's chain.
        var newState = state.copy(activeReplacementChain = continuation.alreadyApplied).pushContinuation(
            DrawReplacementRemainingDrawsContinuation(
                drawingPlayerId = playerId,
                remainingDraws = continuation.drawCount - 1,
                isDrawStep = continuation.isDrawStep,
                announcementApplied = true,
                cycledCardId = continuation.cycledCardId
            )
        )
        val events = mutableListOf<GameEvent>()

        if (response.choice) {
            // Nested events produced by this replacement must not apply it again.
            continuation.declinedIdentity?.let { identity ->
                newState = newState.copy(
                    activeReplacementChain = (newState.activeReplacementChain ?: emptySet()) + identity
                )
            }
            // Player chose to replace the draw - execute the replacement effect
            val effectContext = EffectContext(
                controllerId = playerId,
                sourceId = continuation.sourceId,
                triggeringEntityId = continuation.cycledCardId,
            objectReferences = continuation.objectReferences,
                targets = emptyList(),
                programOrigin = com.wingedsheep.engine.handlers.effects.composite.EffectProgramInterpreter.startedBy(DrawReplacementContinuationResumer::class),
            )
            val effectResult = services.effectExecutorRegistry.execute(
                newState, continuation.replacementEffect, effectContext
            ).toExecutionResult()
            if (effectResult.outcome is Outcome.Paused) {
                return ExecutionResult.propagatePause(
                    effectResult.state,
                    events + effectResult.events
                )
            }
            if (effectResult.outcome is Outcome.Done) {
                newState = effectResult.newState
                events.addAll(effectResult.events)
            }
        } else {
            // Player declined — stamp the declined identity so this specific
            // replacement won't re-prompt (CR 614.5), but other optional
            // replacements (if any) can still prompt for the current draw.
            val declinedId = continuation.declinedIdentity
            val stateWithDeclined = if (declinedId != null) {
                newState.copy(activeReplacementChain = (newState.activeReplacementChain ?: emptySet()) + declinedId)
            } else {
                newState
            }
            val singleDrawExecutor = DrawCardsExecutor(
                cardRegistry = services.cardRegistry,
                effectExecutor = services.effectExecutorRegistry::execute,
                replacementProcessor = services.replacementEffectProcessor,
                amountEvaluator = services.dynamicAmountEvaluator
            )
            // announce = false: this is one card of a draw instruction that already went
            // through the CR 121.2a announcement before it paused on the prompt.
            val singleDrawResult = singleDrawExecutor.executeDraws(
                stateWithDeclined, playerId, 1, announce = false, cycledCardId = continuation.cycledCardId
            ).toExecutionResult()
            if (singleDrawResult.outcome is Outcome.Paused) {
                return ExecutionResult.propagatePause(
                    singleDrawResult.state,
                    events + singleDrawResult.events
                )
            }
            // Clear the chain so remaining draws start clean
            newState = singleDrawResult.newState.copy(activeReplacementChain = null)
            events.addAll(singleDrawResult.events)
        }

        return checkForMore(newState, events)
    }
}
