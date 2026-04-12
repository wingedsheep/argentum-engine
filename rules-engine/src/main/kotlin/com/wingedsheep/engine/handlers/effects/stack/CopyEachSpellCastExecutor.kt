package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingSpellCopy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyEachSpellCastEffect
import kotlin.reflect.KClass

/**
 * Executor for CopyEachSpellCastEffect.
 *
 * Adds a persistent PendingSpellCopy entry to the game state. Unlike CopyNextSpellCastExecutor,
 * the entry is NOT consumed after a single spell — it stays active and copies every instant or
 * sorcery spell the controller casts for the rest of the turn.
 */
class CopyEachSpellCastExecutor : EffectExecutor<CopyEachSpellCastEffect> {

    override val effectType: KClass<CopyEachSpellCastEffect> = CopyEachSpellCastEffect::class

    override fun execute(
        state: GameState,
        effect: CopyEachSpellCastEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: EntityId.generate()
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val pending = PendingSpellCopy(
            controllerId = context.controllerId,
            copies = effect.copies,
            sourceId = sourceId,
            sourceName = sourceName,
            persistent = true
        )
        val newState = state.copy(
            pendingSpellCopies = state.pendingSpellCopies + pending
        )
        return EffectResult.success(newState)
    }
}
