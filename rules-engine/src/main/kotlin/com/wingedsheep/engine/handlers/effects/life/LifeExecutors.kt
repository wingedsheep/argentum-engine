package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService

/**
 * Module providing all life-related effect executors.
 */
class LifeExecutors(
    private val zones: ZoneTransitionService,
    private val amountEvaluator: DynamicAmountEvaluator,
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DrainLifeExecutor(amountEvaluator),
        ExchangeLifeAndStatExecutor(),
        ExchangeLifeTotalsExecutor(cardRegistry, predicateEvaluator = zones.predicateEvaluator),
        RedistributeLifeTotalsExecutor(predicateEvaluator = zones.predicateEvaluator),
        GainLifeExecutor(amountEvaluator),
        LoseLifeExecutor(amountEvaluator),
        OwnerGainsLifeExecutor(predicateEvaluator = zones.predicateEvaluator),
        PayLifeEffectExecutor(zones),
        PayDynamicLifeEffectExecutor(zones, amountEvaluator),
        SetLifeTotalExecutor(amountEvaluator)
    )
}
