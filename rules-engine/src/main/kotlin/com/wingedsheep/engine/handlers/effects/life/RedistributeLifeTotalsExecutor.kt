package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.RedistributeLifeTotalsEffect
import kotlin.reflect.KClass

/**
 * Executor for [RedistributeLifeTotalsEffect] — asks the controller for the first player's new
 * total; [LifeRedistribution] holds the walk and the final application, and
 * [com.wingedsheep.engine.handlers.continuations.RedistributeLifeContinuationResumer] the later steps.
 */
class RedistributeLifeTotalsExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<RedistributeLifeTotalsEffect> {

    override val effectType: KClass<RedistributeLifeTotalsEffect> = RedistributeLifeTotalsEffect::class

    override fun execute(
        state: GameState,
        effect: RedistributeLifeTotalsEffect,
        context: EffectContext
    ): EffectResult = when (val step = LifeRedistribution.start(state, context, predicateEvaluator)) {
        is LifeRedistribution.Step.Done -> EffectResult.success(step.state, step.events)
        is LifeRedistribution.Step.Ask -> EffectResult.from(state.suspendForDecision(step.question, step.continuation))
    }
}
