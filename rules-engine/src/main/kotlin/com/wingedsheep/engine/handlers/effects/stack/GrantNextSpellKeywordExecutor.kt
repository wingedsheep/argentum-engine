package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingNextSpellKeyword
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantNextSpellKeywordEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantNextSpellKeywordEffect].
 *
 * Adds a [PendingNextSpellKeyword] rider to the game state. The granted-keyword resolver reports the
 * keyword on the controller's next matching spell, and
 * [com.wingedsheep.engine.handlers.actions.spell.CastTriggers] consumes the rider on that cast.
 * Mirrors [GrantNextSpellAffinityExecutor].
 */
class GrantNextSpellKeywordExecutor : EffectExecutor<GrantNextSpellKeywordEffect> {

    override val effectType: KClass<GrantNextSpellKeywordEffect> = GrantNextSpellKeywordEffect::class

    override fun execute(
        state: GameState,
        effect: GrantNextSpellKeywordEffect,
        context: EffectContext
    ): EffectResult {
        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val pending = PendingNextSpellKeyword(
            controllerId = context.controllerId,
            keyword = effect.keyword,
            spellFilter = effect.spellFilter,
            sourceId = sourceId,
            sourceName = sourceName
        )
        return EffectResult.success(
            effectiveState.copy(pendingNextSpellKeywords = effectiveState.pendingNextSpellKeywords + pending)
        )
    }
}
