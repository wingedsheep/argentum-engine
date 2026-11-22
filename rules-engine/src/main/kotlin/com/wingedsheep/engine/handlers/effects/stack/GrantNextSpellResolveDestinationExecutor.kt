package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.PendingNextSpellResolveDestination
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantNextSpellResolveDestinationEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantNextSpellResolveDestinationEffect].
 *
 * Adds a [PendingNextSpellResolveDestination] rider to the game state; the controller's next
 * matching cast consumes it (see [com.wingedsheep.engine.handlers.actions.spell.CastTriggers]).
 * Mirrors [GrantNextSpellRiderExecutor].
 */
class GrantNextSpellResolveDestinationExecutor : EffectExecutor<GrantNextSpellResolveDestinationEffect> {

    override val effectType: KClass<GrantNextSpellResolveDestinationEffect> =
        GrantNextSpellResolveDestinationEffect::class

    override fun execute(
        state: GameState,
        effect: GrantNextSpellResolveDestinationEffect,
        context: EffectContext
    ): EffectResult {
        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
        val pending = PendingNextSpellResolveDestination(
            controllerId = context.controllerId,
            destination = effect.destination,
            spellFilter = effect.spellFilter,
            fromZones = effect.fromZones,
            sourceId = sourceId,
            sourceName = sourceName
        )
        return EffectResult.success(
            effectiveState.copy(
                pendingNextSpellResolveDestinations = effectiveState.pendingNextSpellResolveDestinations + pending
            )
        )
    }
}
