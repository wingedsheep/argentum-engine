package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.effects.PayDynamicLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [PayDynamicLifeEffect] — the dynamic, payer-parametric twin of
 * [PayLifeEffectExecutor]. Evaluates [PayDynamicLifeEffect.amount] at resolution, resolves
 * [PayDynamicLifeEffect.payer] (defaulting to the ability controller), then deducts that much life.
 *
 * A computed amount of `<= 0` pays nothing and succeeds (CR 119.4 — paying 0 life is legal), so a
 * gating [com.wingedsheep.sdk.scripting.effects.Gate.MayPay] still proceeds to its `then`.
 */
class PayDynamicLifeEffectExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<PayDynamicLifeEffect> {

    override val effectType: KClass<PayDynamicLifeEffect> = PayDynamicLifeEffect::class

    override fun execute(
        state: GameState,
        effect: PayDynamicLifeEffect,
        context: EffectContext
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        val playerId = TargetResolutionUtils
            .resolvePlayerTarget(EffectTarget.PlayerRef(effect.payer), context, state)
            ?: context.controllerId

        if (state.getEntity(playerId)?.get<LifeTotalComponent>() == null) {
            return EffectResult.error(state, "Player not found for life payment")
        }

        // CR 810.9a — life paid as a cost comes out of the team's shared total.
        val currentLife = state.lifeTotal(playerId)
        val newLife = currentLife - amount
        val newState = state.withLifeTotal(playerId, newLife)

        val events = listOf<EngineGameEvent>(
            LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.PAYMENT)
        )

        val finalState = DamageUtils.markLifeLostThisTurn(newState, playerId)
        return EffectResult.success(finalState, events)
    }
}
