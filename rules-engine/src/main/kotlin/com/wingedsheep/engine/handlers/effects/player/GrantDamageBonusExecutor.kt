package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.DamageBonusComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantDamageBonusEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantDamageBonusEffect.
 * Adds a DamageBonusComponent to the target player, granting a flat damage bonus
 * to matching sources they control for the specified duration.
 */
class GrantDamageBonusExecutor : EffectExecutor<GrantDamageBonusEffect> {

    override val effectType: KClass<GrantDamageBonusEffect> = GrantDamageBonusEffect::class

    override fun execute(
        state: GameState,
        effect: GrantDamageBonusEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for damage bonus grant")

        // Only valid for player targets
        if (!state.turnOrder.contains(targetId)) {
            return ExecutionResult.error(state, "Damage bonus can only be granted to players")
        }

        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            else -> PlayerEffectRemoval.EndOfTurn
        }

        val newState = state.updateEntity(targetId) { container ->
            container.with(DamageBonusComponent(
                bonusAmount = effect.bonusAmount,
                sourceFilter = effect.sourceFilter,
                removeOn = removeOn
            ))
        }

        return ExecutionResult.success(newState)
    }
}
