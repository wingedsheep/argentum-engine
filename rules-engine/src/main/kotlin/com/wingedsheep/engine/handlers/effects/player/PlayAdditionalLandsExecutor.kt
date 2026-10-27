package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.scripting.effects.PlayAdditionalLandsEffect
import kotlin.reflect.KClass

/**
 * Executor for PlayAdditionalLandsEffect.
 * "You may play up to X additional lands this turn."
 *
 * This increases the remaining land drops for the current turn only.
 * The effect does not persist to future turns. The count is evaluated once, as the effect
 * resolves (Nahiri's Lithoforming's X); a count of 0 or less grants nothing.
 */
class PlayAdditionalLandsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<PlayAdditionalLandsEffect> {

    override val effectType: KClass<PlayAdditionalLandsEffect> = PlayAdditionalLandsEffect::class

    override fun execute(
        state: GameState,
        effect: PlayAdditionalLandsEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.controllerId
        val count = amountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) return EffectResult.success(state)

        val currentLandDrops = state.getEntity(playerId)?.get<LandDropsComponent>()
            ?: return EffectResult.error(state, "Player has no LandDropsComponent")

        val newLandDrops = currentLandDrops.copy(
            remaining = currentLandDrops.remaining + count
        )

        val newState = state.updateEntity(playerId) { container ->
            container.with(newLandDrops)
        }

        return EffectResult.success(newState)
    }
}
