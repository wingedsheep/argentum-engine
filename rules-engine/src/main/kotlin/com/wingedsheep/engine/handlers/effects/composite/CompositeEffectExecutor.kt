package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Executor for CompositeEffect.
 * Executes multiple effects in sequence.
 *
 * If a sub-effect pauses for a decision, this executor pushes an EffectContinuation
 * with the remaining effects, so they can be resumed after the decision.
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class CompositeEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<CompositeEffect> {

    override val effectType: KClass<CompositeEffect> = CompositeEffect::class

    override fun execute(
        state: GameState,
        effect: CompositeEffect,
        context: EffectContext
    ): ExecutionResult {
        var currentState = state
        var currentContext = context
        val allEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for ((index, subEffect) in effect.effects.withIndex()) {
            // Calculate remaining effects (everything after current one)
            val remainingEffects = effect.effects.drop(index + 1)

            // Pre-push EffectContinuation for remaining effects BEFORE executing.
            // This ensures that if the sub-effect pushes its own continuation,
            // that continuation ends up on TOP (to be processed first when the response comes).
            // The EffectContinuation will be below it, and checkForMoreContinuations will
            // process it after the sub-effect's continuation is handled.
            val stateForExecution = if (remainingEffects.isNotEmpty()) {
                val continuation = EffectContinuation(
                    decisionId = "pending", // Will be found by checkForMoreContinuations
                    remainingEffects = remainingEffects,
                    sourceId = currentContext.sourceId,
                    controllerId = currentContext.controllerId,
                    opponentId = currentContext.opponentId,
                    xValue = currentContext.xValue,
                    targets = currentContext.targets,
                    storedCollections = currentContext.storedCollections,
                    chosenCreatureType = currentContext.chosenCreatureType,
                    triggeringEntityId = currentContext.triggeringEntityId,
                    namedTargets = currentContext.namedTargets
                )
                currentState.pushContinuation(continuation)
            } else {
                currentState
            }

            val result = effectExecutor(stateForExecution, subEffect, currentContext)

            if (!result.isSuccess && !result.isPaused) {
                // Sub-effect failed - skip it and continue with remaining effects.
                // Per MTG rules, when a spell or ability resolves, you do as much as
                // possible even if some parts can't be performed (e.g., optional target
                // wasn't selected, or target became invalid).
                currentState = if (remainingEffects.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)
                continue
            }

            if (result.isPaused) {
                // Sub-effect needs a decision.
                // Its continuation is on top of the stack.
                // Our pre-pushed EffectContinuation is underneath, ready to be
                // processed by checkForMoreContinuations after the sub-effect resolves.
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            // Effect succeeded - pop our pre-pushed continuation (it wasn't needed)
            currentState = if (remainingEffects.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)

            // Merge any updated collections from the sub-effect into the context
            if (result.updatedCollections.isNotEmpty()) {
                currentContext = currentContext.copy(
                    storedCollections = currentContext.storedCollections + result.updatedCollections
                )
            }
        }

        return ExecutionResult.success(currentState, allEvents)
    }
}
