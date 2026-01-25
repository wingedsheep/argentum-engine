package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.SkipCombatPhasesEffect
import kotlin.reflect.KClass

/**
 * Executor for SkipCombatPhasesEffect.
 * "Target player skips all combat phases of their next turn."
 *
 * This adds a SkipCombatPhasesComponent to the target player. When that player's
 * next turn reaches the combat phase, all combat steps are skipped and the
 * component is removed.
 */
class SkipCombatPhasesExecutor : EffectExecutor<SkipCombatPhasesEffect> {

    override val effectType: KClass<SkipCombatPhasesEffect> = SkipCombatPhasesEffect::class

    override fun execute(
        state: GameState,
        effect: SkipCombatPhasesEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get the target player from the context
        val targetPlayerId = context.targets.firstOrNull()?.let { target ->
            when (target) {
                is ChosenTarget.Player -> target.playerId
                else -> null
            }
        } ?: return ExecutionResult.error(state, "No valid player target for SkipCombatPhasesEffect")

        // Add the SkipCombatPhasesComponent to the target player
        val newState = state.updateEntity(targetPlayerId) { container ->
            container.with(SkipCombatPhasesComponent)
        }

        return ExecutionResult.success(newState)
    }
}