package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.ConditionalEffect
import com.wingedsheep.sdk.scripting.Effect

/**
 * Executor for ConditionalEffect.
 * Executes an effect based on a condition, with optional else branch.
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class ConditionalEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<ConditionalEffect> {

    private val conditionEvaluator = ConditionEvaluator()

    override fun execute(
        state: GameState,
        effect: ConditionalEffect,
        context: EffectContext
    ): ExecutionResult {
        val conditionMet = conditionEvaluator.evaluate(state, effect.condition, context)
        val elseEffect = effect.elseEffect

        return if (conditionMet) {
            effectExecutor(state, effect.effect, context)
        } else if (elseEffect != null) {
            effectExecutor(state, elseEffect, context)
        } else {
            ExecutionResult.success(state)
        }
    }
}
