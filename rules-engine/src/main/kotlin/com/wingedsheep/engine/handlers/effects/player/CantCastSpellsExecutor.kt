package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import kotlin.reflect.KClass

/**
 * Executor for CantCastSpellsEffect.
 * Adds CantCastSpellsComponent to the target player, preventing them from casting spells.
 */
class CantCastSpellsExecutor : EffectExecutor<CantCastSpellsEffect> {

    override val effectType: KClass<CantCastSpellsEffect> = CantCastSpellsEffect::class

    override fun execute(
        state: GameState,
        effect: CantCastSpellsEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for can't cast spells effect")

        if (!state.turnOrder.contains(targetId)) {
            return ExecutionResult.error(state, "Target is not a player")
        }

        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            else -> PlayerEffectRemoval.EndOfTurn
        }

        val newState = state.updateEntity(targetId) { container ->
            container.with(CantCastSpellsComponent(removeOn = removeOn))
        }

        return ExecutionResult.success(newState)
    }
}
