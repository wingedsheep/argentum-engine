package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.cost.PlayerCounterPayment
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.PayExactCountersEffect
import kotlin.reflect.KClass

/** Atomic payment; gates check the same payer and amount before offering the decision. */
class PayExactCountersExecutor(private val amountEvaluator: DynamicAmountEvaluator) : EffectExecutor<PayExactCountersEffect> {
    override val effectType: KClass<PayExactCountersEffect> = PayExactCountersEffect::class

    override fun execute(state: GameState, effect: PayExactCountersEffect, context: EffectContext): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerRef(effect.player, context, state)
            ?: return EffectResult.error(state, "PayExactCounters: could not resolve paying player")
        val amount = amountEvaluator.evaluate(state, effect.amount, context).coerceAtLeast(0)
        val payment = PlayerCounterPayment.pay(state, playerId, effect.counterType, amount)
            ?: return EffectResult.error(state, "Not enough ${effect.counterType.printed} counters to pay $amount")
        return EffectResult.success(payment.first, payment.second)
    }
}
