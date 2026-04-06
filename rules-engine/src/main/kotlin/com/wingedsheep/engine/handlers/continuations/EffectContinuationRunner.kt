package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Executes a list of effects in sequence, handling pauses, errors, and context updates.
 *
 * Used by both the EffectContinuation resumer (decision-resume path) and the EffectContinuation
 * auto-resumer (checkForMoreContinuations path) to avoid duplicating the loop.
 */
class EffectContinuationRunner(
    private val effectExecutorRegistry: EffectExecutorRegistry
) {

    fun executeRemainingEffects(
        initialState: GameState,
        effects: List<Effect>,
        initialContext: EffectContext
    ): ExecutionResult {
        var currentContext = initialContext
        var currentState = initialState
        val allEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for ((index, effect) in effects.withIndex()) {
            val stillRemaining = effects.drop(index + 1)

            val stateForExecution = if (stillRemaining.isNotEmpty()) {
                val remainingContinuation = EffectContinuation(
                    decisionId = "pending",
                    remainingEffects = stillRemaining,
                    effectContext = currentContext
                )
                currentState.pushContinuation(remainingContinuation)
            } else {
                currentState
            }

            val result = effectExecutorRegistry.execute(stateForExecution, effect, currentContext)

            if (!result.isSuccess && !result.isPaused) {
                currentState = if (stillRemaining.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)
                continue
            }

            if (result.isPaused) {
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = if (stillRemaining.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)

            if (result.updatedCollections.isNotEmpty()) {
                currentContext = currentContext.copy(
                    pipeline = currentContext.pipeline.copy(storedCollections = currentContext.pipeline.storedCollections + result.updatedCollections)
                )
            }
        }

        // Return accumulated collections so callers can propagate them
        val accumulatedCollections = currentContext.pipeline.storedCollections - initialContext.pipeline.storedCollections.keys
        return ExecutionResult(currentState, allEvents, updatedCollections = accumulatedCollections)
    }
}
