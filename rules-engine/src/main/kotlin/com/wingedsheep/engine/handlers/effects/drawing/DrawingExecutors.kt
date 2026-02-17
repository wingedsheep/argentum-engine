package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all drawing-related effect executors.
 */
class DrawingExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val targetFinder: TargetFinder = TargetFinder(),
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry? = null
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DrawCardsExecutor(amountEvaluator, cardRegistry),
        DiscardCardsExecutor(decisionHandler),
        DiscardRandomExecutor(),
        DiscardAndChainCopyExecutor(targetFinder, decisionHandler),
        EachOpponentDiscardsExecutor(decisionHandler),
        EachPlayerDiscardsOrLoseLifeExecutor(decisionHandler),
        EachPlayerMayDrawExecutor(decisionHandler),
        HeadGamesExecutor(),
        ReplaceNextDrawWithLifeGainExecutor(),
        ReplaceNextDrawWithBounceExecutor(),
        ReplaceNextDrawWithDiscardExecutor(),
        ReplaceNextDrawWithDamageExecutor(),
        ReplaceNextDrawWithBearTokenExecutor(),
        ReadTheRunesExecutor(amountEvaluator, decisionHandler),
        TradeSecretsExecutor(decisionHandler)
    )
}
