package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.effects.PayDynamicManaCostEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [PayDynamicManaCostEffect] — the dynamic, payer-parametric twin of
 * [PayManaCostExecutor]. It evaluates the effect's [PayDynamicManaCostEffect.amount] at resolution
 * to a count of generic mana, resolves [PayDynamicManaCostEffect.payer] to an actual player
 * (defaulting to the ability controller), then auto-taps that player's mana sources and deducts the
 * generic cost from their pool via the shared [payManaCostFromPool] core.
 *
 * A computed amount of `<= 0` pays nothing and succeeds (the "you chose zero creatures" tail of a
 * "pay {N} for each X" composition). Insufficient mana is a recoverable error — paired with a
 * [com.wingedsheep.sdk.scripting.effects.Gate.MayPay] gate (whose `canAfford` pre-pass already
 * skips an unaffordable selection), this branch is only hit on genuinely degenerate input.
 */
class PayDynamicManaCostExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<PayDynamicManaCostEffect> {

    override val effectType: KClass<PayDynamicManaCostEffect> = PayDynamicManaCostEffect::class

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: PayDynamicManaCostEffect,
        context: EffectContext
    ): EffectResult {
        val amount = dynamicAmountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        val playerId = TargetResolutionUtils
            .resolvePlayerTarget(EffectTarget.PlayerRef(effect.payer), context, state)
            ?: context.controllerId

        return payManaCostFromPool(state, playerId, dynamicManaCost(amount, effect.color), cardRegistry)
    }

    companion object {
        /**
         * Build the [ManaCost] for a [PayDynamicManaCostEffect]: `amount` generic mana when
         * [color] is null, or `amount` copies of the colored symbol (`{G}{G}…`) otherwise.
         */
        fun dynamicManaCost(amount: Int, color: com.wingedsheep.sdk.core.Color?): ManaCost =
            if (color != null) ManaCost.parse("{${color.symbol}}".repeat(amount))
            else ManaCost.parse("{$amount}")
    }
}
