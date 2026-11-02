package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.MaximumHandSizeRemovedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RemoveMaximumHandSizeEffect
import kotlin.reflect.KClass

/**
 * Resolves [RemoveMaximumHandSizeEffect].
 *
 * Stamps [PlayerNoMaximumHandSizeComponent] on the target player with the current timestamp. The
 * property lasts for the rest of the game — or, for [Duration.UntilYourNextTurn], until the effect
 * controller's next untap step ([com.wingedsheep.engine.core.CleanupPhaseManager.expireUntilYourNextTurnEffects]); conferring it again restamps it, since each application
 * is a new effect ordered by its own timestamp (CR 613.11).
 * [com.wingedsheep.engine.core.MaximumHandSize.effective] reads it when discarding to hand size.
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

        // A repeat application is a new effect with a new timestamp (CR 613.7), so it restamps the
        // marker — it then overrides any "maximum hand size is N" static that entered in between.
        // A rest-of-game grant is never downgraded by a later "until your next turn" one; a
        // rest-of-game grant does replace a temporary one.
        val existing = playerContainer.get<PlayerNoMaximumHandSizeComponent>()
        val component = when (effect.duration) {
            Duration.UntilYourNextTurn ->
                // Keep the rest-of-game grant's own timestamp: it must not overtake a
                // "maximum hand size is N" static that entered after it once this one lapses.
                if (existing?.removeOn == PlayerEffectRemoval.Permanent) {
                    existing
                } else {
                    PlayerNoMaximumHandSizeComponent(
                        timestamp = state.timestamp,
                        removeOn = PlayerEffectRemoval.UntilYourNextTurn,
                        expiresForPlayerId = context.controllerId
                    )
                }
            else -> PlayerNoMaximumHandSizeComponent(state.timestamp)
        }
        val newState = state.updateEntity(targetId) { container ->
            container.with(component)
        }.tick()

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
