package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.TapForManaGrant
import com.wingedsheep.engine.state.components.player.TapForManaGrantsComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.TapForManaPermanentsYouDontControlEffect
import kotlin.reflect.KClass

/**
 * Records a turn-scoped "you may tap [filter]s you don't control for mana" permission on the
 * target player (Piracy). Appends to the player's [TapForManaGrantsComponent]; a second grant the
 * same turn stacks and all of them expire together. Shaped like
 * [GrantInstantSpeedLoyaltyAbilitiesExecutor].
 */
class TapForManaPermanentsYouDontControlExecutor : EffectExecutor<TapForManaPermanentsYouDontControlEffect> {

    override val effectType: KClass<TapForManaPermanentsYouDontControlEffect> =
        TapForManaPermanentsYouDontControlEffect::class

    override fun execute(
        state: GameState,
        effect: TapForManaPermanentsYouDontControlEffect,
        context: EffectContext
    ): EffectResult {
        val targetIds = context.resolvePlayerTargets(effect.target, state)
            .filter { state.turnOrder.contains(it) }
        if (targetIds.isEmpty()) {
            return EffectResult.error(state, "No valid target for tap-for-mana grant")
        }

        val incomingRemoveOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            else -> PlayerEffectRemoval.EndOfTurn
        }

        val newState = targetIds.fold(state) { acc, targetId ->
            acc.updateEntity(targetId) { container ->
                val existing = container.get<TapForManaGrantsComponent>()
                // Never demote a Permanent grant because a later end-of-turn grant landed.
                val removeOn = if (existing?.removeOn == PlayerEffectRemoval.Permanent ||
                    incomingRemoveOn == PlayerEffectRemoval.Permanent
                ) PlayerEffectRemoval.Permanent else PlayerEffectRemoval.EndOfTurn
                container.with(
                    TapForManaGrantsComponent(
                        grants = (existing?.grants ?: emptyList()) +
                            TapForManaGrant(effect.permanentFilter, effect.restriction),
                        removeOn = removeOn,
                    )
                )
            }
        }

        return EffectResult.success(newState)
    }
}
