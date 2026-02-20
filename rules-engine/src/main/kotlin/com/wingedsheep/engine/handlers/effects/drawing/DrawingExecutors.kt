package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing all drawing-related effect executors.
 *
 * Uses deferred initialization for DrawCardsExecutor so it can access
 * the parent registry's execute function (needed for pipeline execution
 * of draw replacement effects like Words of Wind).
 */
class DrawingExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val targetFinder: TargetFinder = TargetFinder(),
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry? = null
) : ExecutorModule {
    private var effectExecutor: ((GameState, Effect, EffectContext) -> ExecutionResult)? = null

    private val drawCardsExecutor by lazy {
        DrawCardsExecutor(amountEvaluator, cardRegistry, effectExecutor)
    }

    private val eachPlayerReturnsPermanentToHandExecutor by lazy {
        EachPlayerReturnsPermanentToHandExecutor(effectExecutor)
    }

    /**
     * Initialize the module with the parent registry's execute function.
     * Must be called before executors() is accessed for the first time.
     */
    fun initialize(executor: (GameState, Effect, EffectContext) -> ExecutionResult) {
        this.effectExecutor = executor
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        drawCardsExecutor,
        DiscardAndChainCopyExecutor(targetFinder, decisionHandler),
        EachOpponentDiscardsExecutor(decisionHandler),
        eachPlayerReturnsPermanentToHandExecutor,
        EachPlayerDiscardsOrLoseLifeExecutor(decisionHandler),
        EachPlayerMayDrawExecutor(decisionHandler),
        ReplaceNextDrawWithExecutor(),
        ReadTheRunesExecutor(amountEvaluator, decisionHandler),
        TradeSecretsExecutor(decisionHandler)
    )
}
