package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for LoseLifeEffect.
 * "You lose X life" or "Target player loses X life"
 */
class LoseLifeExecutor : EffectExecutor<LoseLifeEffect> {

    override val effectType: KClass<LoseLifeEffect> = LoseLifeEffect::class

    override fun execute(
        state: GameState,
        effect: LoseLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for life loss")

        val currentLife = state.getEntity(targetId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Target has no life total")

        val newLife = currentLife - effect.amount
        val newState = state.updateEntity(targetId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return ExecutionResult.success(
            newState,
            listOf(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
        )
    }
}
