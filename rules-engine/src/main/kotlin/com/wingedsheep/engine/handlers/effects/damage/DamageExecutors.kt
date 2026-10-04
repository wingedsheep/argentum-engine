package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService

/**
 * Module providing all damage-related effect executors.
 */
class DamageExecutors(
    private val zones: ZoneTransitionService,
    private val amountEvaluator: DynamicAmountEvaluator,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DealDamageExecutor(zones, amountEvaluator),
        DealDamagePerEntityInZoneExecutor(zones),
        DividedDamageExecutor(zones, decisionHandler, amountEvaluator = amountEvaluator),
        DistributeDamageAmongCollectionExecutor(zones, amountEvaluator),
        FightEffectExecutor(zones),
        AmplifyDamageThisTurnExecutor(amountEvaluator),
        DoubleDamageToPlayerExecutor(),
        DamageCantBePreventedThisTurnExecutor(),
        DamageToTargetCantBePreventedThisTurnExecutor()
    )
}
