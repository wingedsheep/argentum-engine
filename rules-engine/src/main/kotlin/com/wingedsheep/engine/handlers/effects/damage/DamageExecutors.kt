package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all damage-related effect executors.
 */
class DamageExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DealDamageExecutor(),
        DealDynamicDamageExecutor(amountEvaluator),
        DealXDamageExecutor(),
        DealXDamageToAllExecutor(),
        DealDamageToAllCreaturesExecutor(),
        DealDamageToAllExecutor()
    )
}
