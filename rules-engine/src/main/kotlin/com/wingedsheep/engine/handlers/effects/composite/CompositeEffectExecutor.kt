package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.Effect
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
        val allEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for ((index, subEffect) in effect.effects.withIndex()) {
            val result = effectExecutor(currentState, subEffect, context)

            if (!result.isSuccess && !result.isPaused) {
                // Error occurred - return it with accumulated events
                return ExecutionResult(
                    state = result.state,
                    events = allEvents + result.events,
                    error = result.error
                )
            }

            if (result.isPaused) {
                // Sub-effect needs a decision
                // Calculate remaining effects (everything after current one)
                val remainingEffects = effect.effects.drop(index + 1)

                // If there are more effects, push a continuation
                var stateForPause = result.state
                if (remainingEffects.isNotEmpty()) {
                    val continuation = EffectContinuation(
                        decisionId = result.pendingDecision!!.id,
                        remainingEffects = remainingEffects,
                        sourceId = context.sourceId,
                        controllerId = context.controllerId,
                        opponentId = context.opponentId,
                        xValue = context.xValue
                    )
                    stateForPause = stateForPause.pushContinuation(continuation)
                }

                return ExecutionResult.paused(
                    stateForPause,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            // Effect succeeded - continue to next
            currentState = result.state
            allEvents.addAll(result.events)
        }

        return ExecutionResult.success(currentState, allEvents)
    }
}
