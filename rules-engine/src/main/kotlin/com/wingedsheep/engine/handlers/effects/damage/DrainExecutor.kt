package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.DrainEffect

/**
 * Executor for DrainEffect.
 * "Deal X damage to target and you gain X life"
 */
class DrainExecutor : EffectExecutor<DrainEffect> {

    override fun execute(
        state: GameState,
        effect: DrainEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for drain")

        // Deal damage
        val damageResult = dealDamageToTarget(state, targetId, effect.amount, context.sourceId)

        // Gain life
        val controllerId = context.controllerId
        val currentLife = damageResult.newState.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: 0
        val newLife = currentLife + effect.amount

        val newState = damageResult.newState.updateEntity(controllerId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        val events = damageResult.events + LifeChangedEvent(
            controllerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN
        )

        return ExecutionResult.success(newState, events)
    }
}
