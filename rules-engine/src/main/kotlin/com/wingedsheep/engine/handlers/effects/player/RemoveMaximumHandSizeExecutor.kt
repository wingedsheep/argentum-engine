package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.MaximumHandSizeRemovedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.sdk.scripting.effects.RemoveMaximumHandSizeEffect
import kotlin.reflect.KClass

/**
 * Resolves [RemoveMaximumHandSizeEffect].
 *
 * Adds [PlayerNoMaximumHandSizeComponent] to the target player. Idempotent: if the player already
 * has no maximum hand size, nothing changes and no event fires (the property is permanent for the
 * rest of the game — conferring it twice is a no-op). [com.wingedsheep.engine.core.CleanupPhaseManager]
 * consults this component when discarding to hand size.
 */
class RemoveMaximumHandSizeExecutor : EffectExecutor<RemoveMaximumHandSizeEffect> {

    override val effectType: KClass<RemoveMaximumHandSizeEffect> = RemoveMaximumHandSizeEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveMaximumHandSizeEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for remove-maximum-hand-size")

        if (!state.turnOrder.contains(targetId)) {
            return EffectResult.error(state, "Remove-maximum-hand-size target must be a player")
        }

        val playerContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target player no longer exists")

        if (playerContainer.has<PlayerNoMaximumHandSizeComponent>()) {
            return EffectResult.success(state)
        }

        val newState = state.updateEntity(targetId) { container ->
            container.with(PlayerNoMaximumHandSizeComponent)
        }

        val playerName = playerContainer.get<PlayerComponent>()?.name ?: "Player"
        val sourceName = context.sourceId?.let {
            state.getEntity(it)?.get<CardComponent>()?.name
        } ?: "Unknown"

        return EffectResult.success(
            newState,
            listOf(MaximumHandSizeRemovedEvent(targetId, playerName, sourceName))
        )
    }
}
