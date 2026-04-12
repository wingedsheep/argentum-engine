package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantCastCreaturesFromGraveyardWithForageEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantCastCreaturesFromGraveyardWithForageEffect.
 * Adds MayCastCreaturesFromGraveyardWithForageComponent to the controller's player entity.
 */
class GrantCastCreaturesFromGraveyardWithForageExecutor :
    EffectExecutor<GrantCastCreaturesFromGraveyardWithForageEffect> {

    override val effectType: KClass<GrantCastCreaturesFromGraveyardWithForageEffect> =
        GrantCastCreaturesFromGraveyardWithForageEffect::class

    override fun execute(
        state: GameState,
        effect: GrantCastCreaturesFromGraveyardWithForageEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.controllerId
        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            else -> PlayerEffectRemoval.EndOfTurn
        }

        val newState = state.updateEntity(playerId) { container ->
            container.with(
                MayCastCreaturesFromGraveyardWithForageComponent(
                    sourceId = context.sourceId,
                    removeOn = removeOn
                )
            )
        }

        return EffectResult.success(newState)
    }
}
