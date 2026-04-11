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
                    effectContext = currentContext
                )
                currentState.pushContinuation(continuation)
            } else {
                currentState
            }

            val result = effectExecutor(stateForExecution, subEffect, currentContext)

            if (!result.isSuccess && !result.isPaused) {
                if (effect.stopOnError) {
                    // Cost-then-payoff composite: if the cost fails, abort remaining effects.
                    // Used by OptionalCostEffect where paying the cost is mandatory for the
                    // payoff — if you can't pay {W}{B}, you don't get the reanimation.
                    val cleanState = if (remainingEffects.isNotEmpty()) {
                        val (_, stateWithoutCont) = result.state.popContinuation()
                        stateWithoutCont
                    } else {
                        result.state
                    }
                    return ExecutionResult.success(cleanState, allEvents + result.events)
                }
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

            // Merge any updated collections / subtype groups from the sub-effect into the context
            if (result.updatedCollections.isNotEmpty() || result.updatedSubtypeGroups.isNotEmpty()) {
                currentContext = currentContext.copy(
                    pipeline = currentContext.pipeline.copy(
                        storedCollections = currentContext.pipeline.storedCollections + result.updatedCollections,
                        storedSubtypeGroups = currentContext.pipeline.storedSubtypeGroups + result.updatedSubtypeGroups
                    )
                )
            }
        }

        // Return accumulated collections / subtype groups so parent composites can see them
        val accumulatedCollections = currentContext.pipeline.storedCollections - context.pipeline.storedCollections.keys
        val accumulatedSubtypeGroups = currentContext.pipeline.storedSubtypeGroups - context.pipeline.storedSubtypeGroups.keys
        return ExecutionResult(
            currentState,
            allEvents,
            updatedCollections = accumulatedCollections,
            updatedSubtypeGroups = accumulatedSubtypeGroups
        )
    }
}
