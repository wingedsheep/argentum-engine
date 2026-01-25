package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.GainLifeEffect

/**
 * Executor for GainLifeEffect.
 * "You gain X life" or "Target player gains X life"
 */
class GainLifeExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<GainLifeEffect> {

    override fun execute(
        state: GameState,
        effect: GainLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for life gain")

        val currentLife = state.getEntity(targetId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Target has no life total")

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        val newLife = currentLife + amount
        val newState = state.updateEntity(targetId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return ExecutionResult.success(
            newState,
            listOf(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
        )
    }
}
