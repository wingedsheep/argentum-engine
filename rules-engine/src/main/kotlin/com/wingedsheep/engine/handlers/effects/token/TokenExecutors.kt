package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.TokenArtRegistry

/**
 * Module providing all token-related effect executors.
 *
 * [tokenArtRegistry] is optional: a null registry just means created tokens fall through to the
 * engine-wide generic art instead of the minting set's own printing. Contexts that never render
 * (AI simulation, gym rollouts) leave it out.
 */
class TokenExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry,
    private val tokenArtRegistry: TokenArtRegistry? = null
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        CreateTokenExecutor(amountEvaluator, staticAbilityHandler, cardRegistry, tokenArtRegistry),
        CreatePredefinedTokenExecutor(cardRegistry, staticAbilityHandler, tokenArtRegistry = tokenArtRegistry),
        CreateRoleTokenExecutor(cardRegistry, staticAbilityHandler),
        CreateTokenCopyOfSourceExecutor(cardRegistry, staticAbilityHandler),
        CreateTokenCopyOfEquippedCreatureExecutor(cardRegistry, staticAbilityHandler),
        CreateTokenCopyOfChosenPermanentExecutor(cardRegistry, staticAbilityHandler),
        CreateTokenCopyOfTargetExecutor(amountEvaluator, staticAbilityHandler, cardRegistry),
        CreateRandomCreatureTokenWithManaValueExecutor(amountEvaluator, staticAbilityHandler, cardRegistry)
    )
}
