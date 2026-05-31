package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.CanAttackDespiteDefenderThisTurnComponent
import com.wingedsheep.sdk.scripting.effects.CanAttackThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [CanAttackThisTurnEffect].
 *
 * Adds [CanAttackDespiteDefenderThisTurnComponent] to the target creature, letting it
 * attack this turn as though it didn't have defender. The marker is honored by the
 * defender attack-restriction rule and removed at end of turn during cleanup.
 */
class CanAttackThisTurnExecutor : EffectExecutor<CanAttackThisTurnEffect> {

    override val effectType: KClass<CanAttackThisTurnEffect> = CanAttackThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: CanAttackThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val newState = state.updateEntity(targetId) { it.with(CanAttackDespiteDefenderThisTurnComponent) }
        return EffectResult.success(newState)
    }
}
