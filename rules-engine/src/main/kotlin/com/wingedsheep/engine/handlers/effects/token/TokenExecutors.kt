package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
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
    private val zones: ZoneTransitionService,
    private val executeEffect: (com.wingedsheep.engine.state.GameState, com.wingedsheep.sdk.scripting.effects.Effect, com.wingedsheep.engine.handlers.EffectContext) -> com.wingedsheep.engine.core.EffectResult,
    private val amountEvaluator: DynamicAmountEvaluator,
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry,
    private val tokenArtRegistry: TokenArtRegistry? = null,
    private val targetFinder: TargetFinder
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> {
        val createPredefinedToken = CreatePredefinedTokenExecutor(
            cardRegistry, staticAbilityHandler, tokenArtRegistry = tokenArtRegistry, amountEvaluator = amountEvaluator
        )
        return listOf(
            CreateTokenBatchExecutor(executeEffect, amountEvaluator, cardRegistry, staticAbilityHandler ?: StaticAbilityHandler(cardRegistry)),
            CreateTokenExecutor(amountEvaluator, staticAbilityHandler, cardRegistry, tokenArtRegistry),
            createPredefinedToken,
            InvestigateExecutor(createPredefinedToken, amountEvaluator),
            CreateAuraTokenExecutor(zones, cardRegistry, staticAbilityHandler),
            CreateTokenCopyOfSourceExecutor(cardRegistry, staticAbilityHandler, predicateEvaluator = zones.predicateEvaluator),
            CreateTokenCopyOfEquippedCreatureExecutor(cardRegistry, staticAbilityHandler, predicateEvaluator = zones.predicateEvaluator),
            CreateTokenCopyOfChosenPermanentExecutor(cardRegistry, staticAbilityHandler, predicateEvaluator = zones.predicateEvaluator),
            CreateTokenCopyOfTargetExecutor(amountEvaluator, staticAbilityHandler, cardRegistry, targetFinder = targetFinder),
            CreateRandomCreatureTokenWithManaValueExecutor(amountEvaluator, staticAbilityHandler, cardRegistry)
        ).map { simultaneous(it) }
    }

    /** Every token-creation executor is one instruction, including a heterogeneous batch. */
    private fun <T : com.wingedsheep.sdk.scripting.effects.Effect> simultaneous(
        delegate: EffectExecutor<T>
    ): EffectExecutor<T> = object : EffectExecutor<T> by delegate {
        override fun execute(
            state: com.wingedsheep.engine.state.GameState,
            effect: T,
            context: com.wingedsheep.engine.handlers.EffectContext
        ): com.wingedsheep.engine.core.EffectResult {
            val existingGroup = TokenEntryGroups.group(context)
            val (group, scopedState) = if (existingGroup != null) existingGroup to state else state.newRoutingId()
            val scopedContext = TokenEntryGroups.withGroup(context, group)
            return TokenEntryGroups.stamp(delegate.execute(scopedState, effect, scopedContext), group)
        }
    }
}
