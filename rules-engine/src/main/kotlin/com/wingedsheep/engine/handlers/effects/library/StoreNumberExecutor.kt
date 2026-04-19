package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.StoreNumberEffect
import kotlin.reflect.KClass

/**
 * Executor for [StoreNumberEffect].
 *
 * Evaluates the [DynamicAmount][com.wingedsheep.sdk.scripting.values.DynamicAmount] once
 * and emits the result via [EffectResult.updatedStoredNumbers]. The surrounding
 * [com.wingedsheep.engine.handlers.effects.composite.CompositeEffectExecutor] merges the
 * value into the pipeline context so later sub-effects can read it with
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.VariableReference].
 */
class StoreNumberExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<StoreNumberEffect> {

    override val effectType: KClass<StoreNumberEffect> = StoreNumberEffect::class

    override fun execute(
        state: GameState,
        effect: StoreNumberEffect,
        context: EffectContext
    ): EffectResult {
        val value = amountEvaluator.evaluate(state, effect.amount, context)
        return EffectResult(
            state = state,
            updatedStoredNumbers = mapOf(effect.name to value)
        )
    }
}
