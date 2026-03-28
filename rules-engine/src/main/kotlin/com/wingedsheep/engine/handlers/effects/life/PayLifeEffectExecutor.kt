package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for PayLifeEffect.
 * Deducts life from the controller as a cost (non-optional).
 */
class PayLifeEffectExecutor : EffectExecutor<PayLifeEffect> {

    override val effectType: KClass<PayLifeEffect> = PayLifeEffect::class

    override fun execute(
        state: GameState,
        effect: PayLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        val currentLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Player not found for life payment")

        val newLife = currentLife - effect.amount
        val newState = state.updateEntity(playerId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        val events = listOf<EngineGameEvent>(
            LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.PAYMENT)
        )

        val finalState = DamageUtils.markLifeLostThisTurn(newState, playerId)
        return ExecutionResult.success(finalState, events)
    }
}
