package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LoyaltyActivationPermission
import com.wingedsheep.engine.state.components.player.LoyaltyActivationPermissionsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantLoyaltyActivationPermissionEffect
import kotlin.reflect.KClass

/**
 * Records a loyalty-activation permission on the target player — instant-speed timing (Jace's
 * Machinations), extra activations per turn (Urza Assembles the Titans), or both.
 *
 * Appends the grant to the player's [LoyaltyActivationPermissionsComponent]; a second grant the
 * same turn stacks and all of them expire together. Permissive
 * mirror of [CantActivateLoyaltyAbilitiesExecutor], shaped like [GrantFlashToSpellsExecutor].
 */
class GrantLoyaltyActivationPermissionExecutor : EffectExecutor<GrantLoyaltyActivationPermissionEffect> {

    override val effectType: KClass<GrantLoyaltyActivationPermissionEffect> =
        GrantLoyaltyActivationPermissionEffect::class

    override fun execute(
        state: GameState,
        effect: GrantLoyaltyActivationPermissionEffect,
        context: EffectContext
    ): EffectResult {
        val targetIds = context.resolvePlayerTargets(effect.target, state)
            .filter { state.turnOrder.contains(it) }
        if (targetIds.isEmpty()) {
            return EffectResult.error(state, "No valid target for loyalty activation permission")
        }

        val incomingRemoveOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            else -> PlayerEffectRemoval.EndOfTurn
        }

        val newState = targetIds.fold(state) { acc, targetId ->
            acc.updateEntity(targetId) { container ->
                val existing = container.get<LoyaltyActivationPermissionsComponent>()
                // Never demote a Permanent grant because a later end-of-turn grant landed — the
                // same bias as GrantFlashToSpellsExecutor.
                val removeOn = if (existing?.removeOn == PlayerEffectRemoval.Permanent ||
                    incomingRemoveOn == PlayerEffectRemoval.Permanent
                ) PlayerEffectRemoval.Permanent else PlayerEffectRemoval.EndOfTurn
                container.with(
                    LoyaltyActivationPermissionsComponent(
                        grants = (existing?.grants ?: emptyList()) + LoyaltyActivationPermission(
                            filter = effect.planeswalkerFilter,
                            instantSpeed = effect.instantSpeed,
                            times = effect.times,
                        ),
                        removeOn = removeOn,
                    )
                )
            }
        }

        return EffectResult.success(newState)
    }
}
