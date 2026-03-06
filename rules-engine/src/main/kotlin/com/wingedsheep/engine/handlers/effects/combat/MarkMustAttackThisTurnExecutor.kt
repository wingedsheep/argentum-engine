package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for MarkMustAttackThisTurnEffect.
 *
 * Adds MustAttackThisTurnComponent to the target entity, marking it as
 * "must attack this turn if able."
 */
class MarkMustAttackThisTurnExecutor : EffectExecutor<MarkMustAttackThisTurnEffect> {

    override val effectType: KClass<MarkMustAttackThisTurnEffect> = MarkMustAttackThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: MarkMustAttackThisTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        val newState = state.updateEntity(targetId) { it.with(MustAttackThisTurnComponent) }
        return ExecutionResult.success(newState)
    }
}
