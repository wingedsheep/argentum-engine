package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.SpellsCantBeCounteredComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantSpellsCantBeCounteredEffect
import kotlin.reflect.KClass

/**
 * Adds a [SpellsCantBeCounteredComponent] entry to the target player, granting
 * counter protection to spells matching the effect's filter for the requested duration.
 *
 * If the player already has the component, the new filter is appended — multiple
 * grants stack additively and all expire together at the duration's end.
 */
class GrantSpellsCantBeCounteredExecutor : EffectExecutor<GrantSpellsCantBeCounteredEffect> {

    override val effectType: KClass<GrantSpellsCantBeCounteredEffect> =
        GrantSpellsCantBeCounteredEffect::class

    override fun execute(
        state: GameState,
        effect: GrantSpellsCantBeCounteredEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolvePlayerTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for grant-can't-be-countered effect")

        if (!state.turnOrder.contains(targetId)) {
            return EffectResult.error(state, "Target is not a player")
        }

        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            else -> PlayerEffectRemoval.EndOfTurn
        }

        val newState = state.updateEntity(targetId) { container ->
            val existing = container.get<SpellsCantBeCounteredComponent>()
            val mergedFilters = (existing?.filters ?: emptyList()) + effect.spellFilter
            container.with(SpellsCantBeCounteredComponent(filters = mergedFilters, removeOn = removeOn))
        }

        return EffectResult.success(newState)
    }
}
