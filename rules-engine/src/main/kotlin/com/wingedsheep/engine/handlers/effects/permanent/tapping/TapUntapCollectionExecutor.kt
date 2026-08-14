package com.wingedsheep.engine.handlers.effects.permanent.tapping

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.untapOrConsumeStun
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import kotlin.reflect.KClass

/**
 * Executor for TapUntapCollectionEffect.
 * Taps or untaps all entities in a named collection.
 */
class TapUntapCollectionExecutor : EffectExecutor<TapUntapCollectionEffect> {

    override val effectType: KClass<TapUntapCollectionEffect> = TapUntapCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: TapUntapCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val entityIds = context.pipeline.storedCollections[effect.collectionName]
            ?: return EffectResult.success(state)

        if (entityIds.isEmpty()) return EffectResult.success(state)

        var currentState = state
        val events = mutableListOf<GameEvent>()

        // The tap/untap atoms own the transition guard (CR 603.2f — no event on a non-transition)
        // and event emission; untapOrConsumeStun also applies stun-counter replacement (CR 122.1d).
        for (entityId in entityIds) {
            if (effect.tap) {
                // Attribute the tap to this effect's controller (see TapUntapExecutor).
                val (next, event) = tap(currentState, entityId, tappedById = context.controllerId)
                currentState = next
                event?.let(events::add)
            } else {
                val (next, untapEvents) = untapOrConsumeStun(currentState, entityId)
                currentState = next
                events.addAll(untapEvents)
            }
        }

        return EffectResult.success(currentState, events)
    }
}
