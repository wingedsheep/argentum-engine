package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingSpellCopy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyNextSpellCastEffect
import kotlin.reflect.KClass

/**
 * Executor for CopyNextSpellCastEffect.
 *
 * Adds a PendingSpellCopy entry to the game state. When the controller next casts
 * an instant or sorcery spell this turn, CastSpellHandler will consume it and
 * create copies of that spell.
 */
class CopyNextSpellCastExecutor : EffectExecutor<CopyNextSpellCastEffect> {

    override val effectType: KClass<CopyNextSpellCastEffect> = CopyNextSpellCastEffect::class

    override fun execute(
        state: GameState,
        effect: CopyNextSpellCastEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: EntityId.generate()
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val pending = PendingSpellCopy(
            controllerId = context.controllerId,
            copies = effect.copies,
            sourceId = sourceId,
            sourceName = sourceName
        )
        val newState = state.copy(
            pendingSpellCopies = state.pendingSpellCopies + pending
        )
        return EffectResult.success(newState)
    }
}
