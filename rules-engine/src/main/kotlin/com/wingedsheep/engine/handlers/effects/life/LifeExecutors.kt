package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all life-related effect executors.
 */
class LifeExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DrainLifeExecutor(amountEvaluator),
        ExchangeLifeAndStatExecutor(),
        ExchangeLifeTotalsExecutor(cardRegistry),
        GainLifeExecutor(amountEvaluator),
        LoseLifeExecutor(amountEvaluator),
        OwnerGainsLifeExecutor(),
        PayLifeEffectExecutor(),
        PayDynamicLifeEffectExecutor(amountEvaluator),
        SetLifeTotalExecutor(amountEvaluator)
    )
}
