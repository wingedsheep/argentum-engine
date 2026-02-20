package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.scripting.effects.PreventLandPlaysThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for PreventLandPlaysThisTurnEffect.
 * Sets the player's remaining land drops to 0, preventing further land plays this turn.
 */
class PreventLandPlaysThisTurnExecutor : EffectExecutor<PreventLandPlaysThisTurnEffect> {

    override val effectType: KClass<PreventLandPlaysThisTurnEffect> = PreventLandPlaysThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: PreventLandPlaysThisTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        val currentLandDrops = state.getEntity(playerId)?.get<LandDropsComponent>()
            ?: return ExecutionResult.error(state, "Player has no LandDropsComponent")

        val newLandDrops = currentLandDrops.copy(remaining = 0)

        val newState = state.updateEntity(playerId) { container ->
            container.with(newLandDrops)
        }

        return ExecutionResult.success(newState)
    }
}
