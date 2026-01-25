package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.LoseHalfLifeEffect

/**
 * Executor for LoseHalfLifeEffect.
 * "Target player loses half their life, rounded up/down"
 */
class LoseHalfLifeExecutor : EffectExecutor<LoseHalfLifeEffect> {

    override fun execute(
        state: GameState,
        effect: LoseHalfLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for half life loss")

        val currentLife = state.getEntity(targetId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Target has no life total")

        val lifeLoss = if (effect.roundUp) {
            (currentLife + 1) / 2
        } else {
            currentLife / 2
        }

        val newLife = currentLife - lifeLoss
        val newState = state.updateEntity(targetId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return ExecutionResult.success(
            newState,
            listOf(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
        )
    }
}
