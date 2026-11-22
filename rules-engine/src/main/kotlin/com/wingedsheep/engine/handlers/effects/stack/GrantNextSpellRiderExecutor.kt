package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingNextSpellRider
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantNextSpellRiderEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantNextSpellRiderEffect].
 *
 * Adds a [PendingNextSpellRider] to the game state. When the controller next casts a matching
 * spell this turn, [com.wingedsheep.engine.handlers.actions.spell.CastTriggers] consumes it and
 * applies its rider to that spell. Mirrors [CopyNextSpellCastExecutor].
 */
class GrantNextSpellRiderExecutor : EffectExecutor<GrantNextSpellRiderEffect> {

    override val effectType: KClass<GrantNextSpellRiderEffect> = GrantNextSpellRiderEffect::class

    override fun execute(
        state: GameState,
        effect: GrantNextSpellRiderEffect,
        context: EffectContext
    ): EffectResult {
        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val pending = PendingNextSpellRider(
            controllerId = context.controllerId,
            spellFilter = effect.spellFilter,
            rider = effect.rider,
            sourceId = sourceId,
            sourceName = sourceName
        )
        return EffectResult.success(
            effectiveState.copy(pendingNextSpellRiders = effectiveState.pendingNextSpellRiders + pending)
        )
    }
}
