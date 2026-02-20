package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for DealDamageEffect.
 * Handles both fixed and dynamic damage amounts.
 */
class DealDamageExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<DealDamageEffect> {

    override val effectType: KClass<DealDamageEffect> = DealDamageEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = EffectExecutorUtils.resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "No valid target for damage")

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return ExecutionResult.success(state)
        }

        // Use damageSource override if specified (e.g., EnchantedCreature for Lavamancer's Skill)
        val damageSourceTarget = effect.damageSource
        val sourceId = if (damageSourceTarget != null) {
            EffectExecutorUtils.resolveTarget(damageSourceTarget, context, state)
        } else {
            context.sourceId
        }

        return dealDamageToTarget(state, targetId, amount, sourceId, effect.cantBePrevented)
    }
}
