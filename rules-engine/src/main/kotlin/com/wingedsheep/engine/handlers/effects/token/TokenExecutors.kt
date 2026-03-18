package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all token-related effect executors.
 */
class TokenExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry? = null
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        CreateTokenExecutor(amountEvaluator, staticAbilityHandler),
        CreateTreasureExecutor(),
        CreateChosenTokenExecutor(amountEvaluator),
        CreateTokenCopyOfSourceExecutor(cardRegistry, staticAbilityHandler),
        CreateTokenCopyOfEquippedCreatureExecutor(cardRegistry, staticAbilityHandler)
    )
}
