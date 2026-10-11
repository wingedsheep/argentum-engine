package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.FlashGrant
import com.wingedsheep.engine.state.components.player.FlashGrantsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantFlashToSpellsEffect
import kotlin.reflect.KClass

/**
 * Records a duration-bounded flash permission on the target player.
 *
 * Appends a [FlashGrant] to the target's [FlashGrantsComponent] — multiple grants stack
 * additively (a second Borne Upon a Wind the same turn), and each expires on its own duration:
 * `EndOfTurn` at cleanup, `UntilYourNextTurn` after the granting player's next untap step
 * (Teferi, Time Raveler's +1), `Permanent` never.
 *
 * Sibling of [GrantSpellsCantBeCounteredExecutor].
 */
class GrantFlashToSpellsExecutor : EffectExecutor<GrantFlashToSpellsEffect> {

    override val effectType: KClass<GrantFlashToSpellsEffect> = GrantFlashToSpellsEffect::class

    override fun execute(
        state: GameState,
        effect: GrantFlashToSpellsEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolvePlayerTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for grant-flash-to-spells effect")

        if (!state.turnOrder.contains(targetId)) {
            return EffectResult.error(state, "Target is not a player")
        }

        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            is Duration.UntilYourNextTurn -> PlayerEffectRemoval.UntilYourNextTurn
            else -> PlayerEffectRemoval.EndOfTurn
        }
        // "Until your next turn" keys off the granting player, which differs from the holder only
        // when the grant targets someone else.
        val grantingPlayer = context.effectControllerId ?: context.controllerId
        val expiresFor = if (removeOn == PlayerEffectRemoval.UntilYourNextTurn && grantingPlayer != targetId)
            grantingPlayer else null
        val grant = FlashGrant(effect.spellFilter, removeOn, expiresFor)

        val newState = state.updateEntity(targetId) { container ->
            val existing = container.get<FlashGrantsComponent>()?.grants ?: emptyList()
            container.with(FlashGrantsComponent(existing + grant))
        }

        return EffectResult.success(newState)
    }
}
