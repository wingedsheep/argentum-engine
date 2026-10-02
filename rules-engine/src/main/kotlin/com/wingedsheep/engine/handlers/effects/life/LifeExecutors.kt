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
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val replacementProcessor: com.wingedsheep.engine.replacement.ReplacementEffectProcessor,
    private val executeEffect: (com.wingedsheep.engine.state.GameState, com.wingedsheep.sdk.scripting.effects.Effect,
        com.wingedsheep.engine.handlers.EffectContext) -> com.wingedsheep.engine.core.EffectResult
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DrainLifeExecutor(amountEvaluator),
        ExchangeLifeAndStatExecutor(zones.predicateEvaluator),
        ExchangeLifeTotalsExecutor(cardRegistry, predicateEvaluator = zones.predicateEvaluator),
        RedistributeLifeTotalsExecutor(predicateEvaluator = zones.predicateEvaluator),
        GainLifeExecutor(amountEvaluator, replacementProcessor, executeEffect),
        LoseLifeExecutor(amountEvaluator),
        OwnerGainsLifeExecutor(predicateEvaluator = zones.predicateEvaluator),
        PayLifeEffectExecutor(zones),
        PayDynamicLifeEffectExecutor(zones, amountEvaluator),
        SetLifeTotalExecutor(amountEvaluator)
    )
}
