package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingFreeCastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantFreeCastThisTurnEffect].
 *
 * Adds a [PendingFreeCastSpell] permission to the game state.
 * [com.wingedsheep.engine.mechanics.mana.CostCalculator.hasFreeCastPermission] reads it so a
 * matching spell is offered as a `CastSpell.useWithoutPayingManaCost` action. A `nextSpellOnly`
 * rider is consumed by the next matching cast (whether or not the free cast was taken); a
 * whole-turn one stays until [com.wingedsheep.engine.core.TurnManager.startTurn] clears it.
 * Mirrors [GrantNextSpellAffinityExecutor].
 */
class GrantFreeCastThisTurnExecutor : EffectExecutor<GrantFreeCastThisTurnEffect> {

    override val effectType: KClass<GrantFreeCastThisTurnEffect> = GrantFreeCastThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: GrantFreeCastThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val pending = PendingFreeCastSpell(
            controllerId = context.controllerId,
            spellFilter = effect.spellFilter,
            nextSpellOnly = effect.nextSpellOnly,
            fromZones = effect.fromZones,
            sourceId = sourceId,
            sourceName = sourceName
        )
        val newState = effectiveState.copy(
            pendingFreeCastSpells = effectiveState.pendingFreeCastSpells + pending
        )
        return EffectResult.success(newState)
    }
}
