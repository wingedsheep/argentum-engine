package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all damage-related effect executors.
 */
class DamageExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : ExecutorModule {
    @Suppress("DEPRECATION")
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DealDamageExecutor(),
        DealDynamicDamageExecutor(amountEvaluator),
        DealXDamageExecutor(),
        DealXDamageToAllExecutor(),
        DealDamageToAllCreaturesExecutor(),
        DealDamageToAllExecutor(),
        DealDamageToGroupExecutor(amountEvaluator),
        DealDamageToPlayersExecutor(amountEvaluator),
        DividedDamageExecutor(decisionHandler)
    )
}
