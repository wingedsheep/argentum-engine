package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing all drawing-related effect executors.
 *
 * DrawCardsExecutor runs draw-replacement pipelines (Words of Wind) through the parent
 * registry's execute function, which the registry hands in at construction.
 */
class DrawingExecutors(
    /** The registry's re-entrant entry point, for the executors that run sub-effects. */
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val zones: ZoneTransitionService,
    private val amountEvaluator: DynamicAmountEvaluator,
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val targetFinder: TargetFinder,
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val replacementProcessor: com.wingedsheep.engine.replacement.ReplacementEffectProcessor
) : ExecutorModule {
    private val drawCardsExecutor = DrawCardsExecutor(amountEvaluator, cardRegistry, effectExecutor, replacementProcessor)

    private val eachPlayerReturnsPermanentToHandExecutor = EachPlayerReturnsPermanentToHandExecutor(effectExecutor)

    override fun executors(): List<EffectExecutor<*>> = listOf(
        drawCardsExecutor,
        DrawUpToExecutor(decisionHandler),
        eachPlayerReturnsPermanentToHandExecutor,
        EachPlayerDiscardsOrLoseLifeExecutor(effectExecutor),
        ReplaceNextDrawWithExecutor(),
        EachPlayerDrawsForDamageDealtToSourceExecutor(drawCardsExecutor),
    )
}
