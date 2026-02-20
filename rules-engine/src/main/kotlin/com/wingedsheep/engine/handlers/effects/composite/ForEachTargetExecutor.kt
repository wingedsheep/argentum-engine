package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for ForEachTargetEffect.
 *
 * Iterates over each target in the context, executing the sub-effects pipeline
 * for each target individually. Each iteration gets a fresh context with only
 * the current target as ContextTarget(0) and cleared storedCollections.
 *
 * If a sub-effect pauses (e.g., for a reorder decision), a ForEachTargetContinuation
 * is pre-pushed with the remaining targets so execution resumes after the decision.
 *
 * Uses the same pre-push/pop pattern as CompositeEffectExecutor to handle
 * sub-effects that themselves push continuations.
 */
class ForEachTargetExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<ForEachTargetEffect> {

    override val effectType: KClass<ForEachTargetEffect> = ForEachTargetEffect::class

    override fun execute(
        state: GameState,
        effect: ForEachTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        if (context.targets.isEmpty()) {
            return ExecutionResult.success(state)
        }

        return processTargets(state, effect.effects, context.targets, context)
    }

    /**
     * Process targets starting from the first one in the list.
     * Called both from the executor and from the continuation handler.
     */
    fun processTargets(
        state: GameState,
        effects: List<Effect>,
        targets: List<ChosenTarget>,
        outerContext: EffectContext
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, target) in targets.withIndex()) {
            val remainingTargets = targets.drop(index + 1)

            // Create a per-target context with only this one target and fresh collections
            val perTargetContext = outerContext.copy(
                targets = listOf(target),
                storedCollections = emptyMap()
            )

            // Pre-push a ForEachTargetContinuation for remaining targets
            val stateForExecution = if (remainingTargets.isNotEmpty()) {
                val continuation = ForEachTargetContinuation(
                    decisionId = "pending",
                    remainingTargets = remainingTargets,
                    effects = effects,
                    sourceId = outerContext.sourceId,
                    controllerId = outerContext.controllerId,
                    opponentId = outerContext.opponentId,
                    xValue = outerContext.xValue
                )
                currentState.pushContinuation(continuation)
            } else {
                currentState
            }

            // Execute the sub-effects pipeline for this target
            val result = executeSubEffects(stateForExecution, effects, perTargetContext)

            if (result.isPaused) {
                // Sub-effect needs a decision. ForEachTargetContinuation is underneath.
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            // Pop the pre-pushed continuation (it wasn't needed)
            currentState = if (remainingTargets.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Execute a list of sub-effects in sequence for a single target.
     * Same pattern as CompositeEffectExecutor.
     */
    private fun executeSubEffects(
        state: GameState,
        effects: List<Effect>,
        context: EffectContext
    ): ExecutionResult {
        var currentState = state
        var currentContext = context
        val allEvents = mutableListOf<GameEvent>()

        for ((index, subEffect) in effects.withIndex()) {
            val remainingEffects = effects.drop(index + 1)

            // Pre-push EffectContinuation for remaining sub-effects
            val stateForExecution = if (remainingEffects.isNotEmpty()) {
                val continuation = EffectContinuation(
                    decisionId = "pending",
                    remainingEffects = remainingEffects,
                    sourceId = currentContext.sourceId,
                    controllerId = currentContext.controllerId,
                    opponentId = currentContext.opponentId,
                    xValue = currentContext.xValue,
                    targets = currentContext.targets,
                    storedCollections = currentContext.storedCollections
                )
                currentState.pushContinuation(continuation)
            } else {
                currentState
            }

            val result = effectExecutor(stateForExecution, subEffect, currentContext)

            if (!result.isSuccess && !result.isPaused) {
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
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = if (remainingEffects.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)

            if (result.updatedCollections.isNotEmpty()) {
                currentContext = currentContext.copy(
                    storedCollections = currentContext.storedCollections + result.updatedCollections
                )
            }
        }

        return ExecutionResult.success(currentState, allEvents)
    }
}
