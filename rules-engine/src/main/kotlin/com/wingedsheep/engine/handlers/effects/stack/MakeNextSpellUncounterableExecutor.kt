package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingUncounterableSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.MakeNextSpellUncounterableEffect
import kotlin.reflect.KClass

/**
 * Executor for [MakeNextSpellUncounterableEffect].
 *
 * Adds a [PendingUncounterableSpell] rider to the game state. When the controller next casts
 * a matching spell this turn, [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler]
 * consumes it and stamps that spell uncounterable. Mirrors [CopyNextSpellCastExecutor].
 */
class MakeNextSpellUncounterableExecutor : EffectExecutor<MakeNextSpellUncounterableEffect> {

    override val effectType: KClass<MakeNextSpellUncounterableEffect> =
        MakeNextSpellUncounterableEffect::class

    override fun execute(
        state: GameState,
        effect: MakeNextSpellUncounterableEffect,
        context: EffectContext
    ): EffectResult {
        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val pending = PendingUncounterableSpell(
            controllerId = context.controllerId,
            spellFilter = effect.spellFilter,
            sourceId = sourceId,
            sourceName = sourceName
        )
        val newState = effectiveState.copy(
            pendingUncounterableSpells = effectiveState.pendingUncounterableSpells + pending
        )
        return EffectResult.success(newState)
    }
}
