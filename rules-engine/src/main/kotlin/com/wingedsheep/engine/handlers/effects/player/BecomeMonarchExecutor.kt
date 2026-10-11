package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.monarch.MonarchService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.BecomeMonarchEffect
import kotlin.reflect.KClass

/**
 * Resolves [BecomeMonarchEffect] — "you become the monarch", and the inherent combat-damage steal
 * "its controller becomes the monarch" (CR 725.2), which is the same effect aimed at the damaging
 * creature's controller. The title moves through [MonarchService.become] (CR 725.3).
 *
 * A player who has left the game can't become the monarch; that resolves to nothing rather than
 * crowning a seat that is gone.
 */
class BecomeMonarchExecutor : EffectExecutor<BecomeMonarchEffect> {

    override val effectType: KClass<BecomeMonarchEffect> = BecomeMonarchEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeMonarchEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        if (targetId !in state.activePlayers) return EffectResult.success(state)

        // The inherent steal has the player entity as its source, so it has no card name.
        val sourceName = context.sourceId?.let {
            state.getEntity(it)?.get<CardComponent>()?.name
        } ?: MonarchService.SOURCE_NAME

        val (newState, events) = MonarchService.become(state, targetId, sourceName)
        return EffectResult.success(newState, events)
    }
}
