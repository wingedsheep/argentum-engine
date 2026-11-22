package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingNextSpellAffinity
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantNextSpellAffinityEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantNextSpellAffinityEffect].
 *
 * Adds a [PendingNextSpellAffinity] rider to the game state. The cost calculator reduces the
 * controller's next matching spell by their count of the rider's card type, and
 * [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler] consumes the rider on that cast.
 * Mirrors [GrantNextSpellRiderExecutor].
 */
class GrantNextSpellAffinityExecutor : EffectExecutor<GrantNextSpellAffinityEffect> {

    override val effectType: KClass<GrantNextSpellAffinityEffect> = GrantNextSpellAffinityEffect::class

    override fun execute(
        state: GameState,
        effect: GrantNextSpellAffinityEffect,
        context: EffectContext
    ): EffectResult {
        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val pending = PendingNextSpellAffinity(
            controllerId = context.controllerId,
            spellFilter = effect.spellFilter,
            forType = effect.forType,
            sourceId = sourceId,
            sourceName = sourceName
        )
        val newState = effectiveState.copy(
            pendingNextSpellAffinities = effectiveState.pendingNextSpellAffinities + pending
        )
        return EffectResult.success(newState)
    }
}
