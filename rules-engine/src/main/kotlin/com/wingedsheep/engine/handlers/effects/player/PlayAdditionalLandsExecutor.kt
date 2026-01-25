package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.scripting.PlayAdditionalLandsEffect

/**
 * Executor for PlayAdditionalLandsEffect.
 * "You may play up to X additional lands this turn."
 *
 * This increases the remaining land drops for the current turn only.
 * The effect does not persist to future turns.
 */
class PlayAdditionalLandsExecutor : EffectExecutor<PlayAdditionalLandsEffect> {

    override fun execute(
        state: GameState,
        effect: PlayAdditionalLandsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        val currentLandDrops = state.getEntity(playerId)?.get<LandDropsComponent>()
            ?: return ExecutionResult.error(state, "Player has no LandDropsComponent")

        val newLandDrops = currentLandDrops.copy(
            remaining = currentLandDrops.remaining + effect.count
        )

        val newState = state.updateEntity(playerId) { container ->
            container.with(newLandDrops)
        }

        return ExecutionResult.success(newState)
    }
}
