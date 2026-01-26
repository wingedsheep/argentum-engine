package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all drawing-related effect executors.
 */
class DrawingExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DrawCardsExecutor(amountEvaluator),
        DiscardCardsExecutor(decisionHandler),
        DiscardRandomExecutor(),
        EachPlayerDiscardsDrawsExecutor(decisionHandler),
        EachPlayerDrawsXExecutor(),
        EachPlayerMayDrawExecutor(decisionHandler)
    )
}
