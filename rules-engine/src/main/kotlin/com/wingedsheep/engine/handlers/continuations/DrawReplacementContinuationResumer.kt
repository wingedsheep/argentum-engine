package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.StaticDrawReplacementContinuation
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.replacement.ReplacementEffectIdentity

class DrawReplacementContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(StaticDrawReplacementContinuation::class, ::resumeStaticDrawReplacement)
    )

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
        var newState = state
        val events = mutableListOf<GameEvent>()

        if (response.choice) {
            // Player chose to replace the draw - execute the replacement effect
            val effectContext = EffectContext(
                controllerId = playerId,
                sourceId = continuation.sourceId,
                targets = emptyList()
            )
            val effectResult = services.effectExecutorRegistry.execute(
                newState, continuation.replacementEffect, effectContext
            ).toExecutionResult()
            if (effectResult.isPaused) {
                return ExecutionResult.paused(
                    effectResult.state,
                    effectResult.pendingDecision!!,
                    events + effectResult.events
                )
            }
            if (effectResult.isSuccess) {
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
                replacementProcessor = services.replacementEffectProcessor
            )
            // announce = false: this is one card of a draw instruction that already went
            // through the CR 121.2a announcement before it paused on the prompt.
            val singleDrawResult = singleDrawExecutor.executeDraws(
                stateWithDeclined, playerId, 1, announce = false
            ).toExecutionResult()
            if (singleDrawResult.isPaused) {
                return ExecutionResult.paused(
                    singleDrawResult.state,
                    singleDrawResult.pendingDecision!!,
                    events + singleDrawResult.events
                )
            }
            // Clear the chain so remaining draws start clean
            newState = singleDrawResult.newState.copy(activeReplacementChain = null)
            events.addAll(singleDrawResult.events)
        }

        // Continue remaining draws (drawCount - 1)
        val remainingDraws = continuation.drawCount - 1
        if (remainingDraws > 0) {
            val drawExecutor = DrawCardsExecutor(
                cardRegistry = services.cardRegistry,
                effectExecutor = services.effectExecutorRegistry::execute,
                replacementProcessor = services.replacementEffectProcessor
            )
            val drawResult = if (continuation.isDrawStep) {
                val turnManager = TurnManager(
                    cardRegistry = services.cardRegistry,
                    effectExecutor = services.effectExecutorRegistry::execute,
                    replacementProcessor = services.replacementEffectProcessor
                )
                turnManager.drawCards(newState, playerId, remainingDraws, announce = false)
            } else {
                drawExecutor.executeDraws(newState, playerId, remainingDraws, announce = false).toExecutionResult()
            }
            if (drawResult.isPaused) {
                return ExecutionResult.paused(
                    drawResult.state,
                    drawResult.pendingDecision!!,
                    events + drawResult.events
                )
            }
            newState = drawResult.newState
            events.addAll(drawResult.events)

            // Set priority for draw step
            if (continuation.isDrawStep) {
                newState = newState.withPriority(playerId)
            }
        } else if (continuation.isDrawStep) {
            newState = newState.withPriority(playerId)
        }

        return checkForMore(newState, events)
    }
}
