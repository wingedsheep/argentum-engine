package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
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
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val newState = state.updateEntity(targetId) { it.with(MustAttackThisTurnComponent) }
        return EffectResult.success(newState)
    }
}
