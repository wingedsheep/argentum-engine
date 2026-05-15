package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.CitysBlessingGainedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.sdk.scripting.effects.GainCitysBlessingEffect
import kotlin.reflect.KClass

/**
 * Resolves [GainCitysBlessingEffect].
 *
 * Adds [PlayerCitysBlessingComponent] to the target player. Idempotent: if the
 * player already has the blessing, nothing changes and no event fires (per
 * CR 702.131c the blessing is permanent — granting it twice is a no-op).
 */
class GainCitysBlessingExecutor : EffectExecutor<GainCitysBlessingEffect> {

    override val effectType: KClass<GainCitysBlessingEffect> = GainCitysBlessingEffect::class

    override fun execute(
        state: GameState,
        effect: GainCitysBlessingEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for city's blessing grant")

        if (!state.turnOrder.contains(targetId)) {
            return EffectResult.error(state, "City's blessing target must be a player")
        }

        val playerContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target player no longer exists")

        if (playerContainer.has<PlayerCitysBlessingComponent>()) {
            return EffectResult.success(state)
        }

        val newState = state.updateEntity(targetId) { container ->
            container.with(PlayerCitysBlessingComponent)
        }

        val playerName = playerContainer.get<PlayerComponent>()?.name ?: "Player"
        val sourceName = context.sourceId?.let {
            state.getEntity(it)?.get<CardComponent>()?.name
        } ?: "Unknown"

        return EffectResult.success(
            newState,
            listOf(CitysBlessingGainedEvent(targetId, playerName, sourceName))
        )
    }
}
