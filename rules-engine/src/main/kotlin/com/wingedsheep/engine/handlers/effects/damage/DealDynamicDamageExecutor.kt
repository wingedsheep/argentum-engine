package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.DealDynamicDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for DealDynamicDamageEffect.
 * "Deal damage equal to [dynamic amount] to target"
 */
class DealDynamicDamageExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<DealDynamicDamageEffect> {

    override val effectType: KClass<DealDynamicDamageEffect> = DealDynamicDamageEffect::class

    override fun execute(
        state: GameState,
        effect: DealDynamicDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for dynamic damage")

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return ExecutionResult.success(state)
        }

        return dealDamageToTarget(state, targetId, amount, context.sourceId)
    }
}
