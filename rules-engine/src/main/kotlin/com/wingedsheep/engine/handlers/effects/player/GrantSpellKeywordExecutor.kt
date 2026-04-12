package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.player.SpellKeywordGrant
import com.wingedsheep.sdk.scripting.effects.GrantSpellKeywordEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantSpellKeywordEffect.
 *
 * Adds or updates a GrantedSpellKeywordsComponent on the controller's player entity,
 * granting the specified keyword to spells matching the filter.
 * Used for emblems like "Instant and sorcery spells you cast have storm."
 */
class GrantSpellKeywordExecutor : EffectExecutor<GrantSpellKeywordEffect> {

    override val effectType: KClass<GrantSpellKeywordEffect> = GrantSpellKeywordEffect::class

    override fun execute(
        state: GameState,
        effect: GrantSpellKeywordEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.controllerId
        val playerContainer = state.getEntity(playerId) ?: return EffectResult.success(state)

        val newGrant = SpellKeywordGrant(effect.keyword, effect.spellFilter)

        val existing = playerContainer.get<GrantedSpellKeywordsComponent>()
        val updated = if (existing != null) {
            existing.copy(grants = existing.grants + newGrant)
        } else {
            GrantedSpellKeywordsComponent(listOf(newGrant))
        }

        val newContainer = playerContainer.with(updated)
        val newState = state.withEntity(playerId, newContainer)

        return EffectResult.success(newState)
    }
}
