package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.Effect

/**
 * Executor for CompositeEffect.
 * Executes multiple effects in sequence.
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class CompositeEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<CompositeEffect> {

    override fun execute(
        state: GameState,
        effect: CompositeEffect,
        context: EffectContext
    ): ExecutionResult {
        var result = ExecutionResult.success(state)

        for (subEffect in effect.effects) {
            result = result.andThen { currentState ->
                effectExecutor(currentState, subEffect, context)
            }
            if (!result.isSuccess) break
        }

        return result
    }
}
