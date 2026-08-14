package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
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
    ): EffectResult {
        val (newState, events) = LifePaymentService.pay(state, context.controllerId, effect.amount)
            ?: return EffectResult.error(state, "Player not found for life payment")
        return EffectResult.success(newState, events)
    }
}
