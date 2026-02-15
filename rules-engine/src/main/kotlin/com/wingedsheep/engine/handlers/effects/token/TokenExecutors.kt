package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all token-related effect executors.
 */
class TokenExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        CreateTokenExecutor(),
        CreateTreasureExecutor(),
        CreateDynamicTokensExecutor(amountEvaluator),
        CreateChosenTokenExecutor(amountEvaluator)
    )
}
