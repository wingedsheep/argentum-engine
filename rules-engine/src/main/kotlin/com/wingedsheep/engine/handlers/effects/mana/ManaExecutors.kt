package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all mana-related effect executors.
 */
class ManaExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        AddManaExecutor(amountEvaluator),
        AddColorlessManaExecutor(amountEvaluator),
        AddAnyColorManaExecutor(amountEvaluator)
    )
}
