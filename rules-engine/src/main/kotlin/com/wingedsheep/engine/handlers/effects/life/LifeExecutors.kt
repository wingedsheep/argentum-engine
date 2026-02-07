package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all life-related effect executors.
 */
class LifeExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        GainLifeExecutor(amountEvaluator),
        LoseLifeExecutor(amountEvaluator),
        LoseHalfLifeExecutor(),
        OwnerGainsLifeExecutor(),
        SetLifeTotalForEachPlayerExecutor(amountEvaluator)
    )
}
