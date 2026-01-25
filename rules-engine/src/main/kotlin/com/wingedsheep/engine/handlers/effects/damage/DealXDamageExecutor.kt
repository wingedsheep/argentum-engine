package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.DealXDamageEffect

/**
 * Executor for DealXDamageEffect.
 * "Deal X damage to target creature/player" where X is the spell's X value
 */
class DealXDamageExecutor : EffectExecutor<DealXDamageEffect> {

    override fun execute(
        state: GameState,
        effect: DealXDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for X damage")

        val xValue = context.xValue ?: 0
        return dealDamageToTarget(state, targetId, xValue, context.sourceId)
    }
}
