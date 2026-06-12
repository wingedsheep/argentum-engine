package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CantActivateLoyaltyAbilitiesComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CantActivateLoyaltyAbilitiesEffect
import kotlin.reflect.KClass

/**
 * Executor for [CantActivateLoyaltyAbilitiesEffect].
 * Adds [CantActivateLoyaltyAbilitiesComponent] to the target player, preventing them from
 * activating planeswalkers' loyalty abilities for the effect's duration.
 */
class CantActivateLoyaltyAbilitiesExecutor : EffectExecutor<CantActivateLoyaltyAbilitiesEffect> {

    override val effectType: KClass<CantActivateLoyaltyAbilitiesEffect> = CantActivateLoyaltyAbilitiesEffect::class

    override fun execute(
        state: GameState,
        effect: CantActivateLoyaltyAbilitiesEffect,
        context: EffectContext
    ): EffectResult {
        val targetIds = context.resolvePlayerTargets(effect.target, state)
            .filter { state.turnOrder.contains(it) }
        if (targetIds.isEmpty()) {
            return EffectResult.error(state, "No valid target for can't-activate-loyalty effect")
        }

        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            else -> PlayerEffectRemoval.EndOfTurn
        }

        val newState = targetIds.fold(state) { acc, targetId ->
            acc.updateEntity(targetId) { container ->
                container.with(CantActivateLoyaltyAbilitiesComponent(removeOn = removeOn))
            }
        }

        return EffectResult.success(newState)
    }
}
