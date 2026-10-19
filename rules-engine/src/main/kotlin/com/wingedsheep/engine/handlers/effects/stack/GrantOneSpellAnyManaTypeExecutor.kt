package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingOneSpellAnyManaType
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantOneSpellAnyManaTypeEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantOneSpellAnyManaTypeEffect].
 *
 * Adds a [PendingOneSpellAnyManaType] rider to the game state, spent by
 * [com.wingedsheep.engine.handlers.actions.spell.CastCostPayer] on the first matching spell whose
 * payment needs it. Mirrors [GrantNextSpellKeywordExecutor].
 */
class GrantOneSpellAnyManaTypeExecutor : EffectExecutor<GrantOneSpellAnyManaTypeEffect> {

    override val effectType: KClass<GrantOneSpellAnyManaTypeEffect> = GrantOneSpellAnyManaTypeEffect::class

    override fun execute(
        state: GameState,
        effect: GrantOneSpellAnyManaTypeEffect,
        context: EffectContext
    ): EffectResult {
        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val pending = PendingOneSpellAnyManaType(
            controllerId = context.controllerId,
            spellFilter = effect.spellFilter,
            sourceId = sourceId,
            sourceName = sourceName
        )
        return EffectResult.success(
            effectiveState.copy(pendingAnyManaTypeSpells = effectiveState.pendingAnyManaTypeSpells + pending)
        )
    }
}
