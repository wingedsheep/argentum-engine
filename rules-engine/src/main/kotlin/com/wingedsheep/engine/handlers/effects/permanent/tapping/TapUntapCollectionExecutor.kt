package com.wingedsheep.engine.handlers.effects.permanent.tapping

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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

        for (entityId in entityIds) {
            val container = currentState.getEntity(entityId) ?: continue
            val cardName = container.get<CardComponent>()?.name ?: "Permanent"
            val alreadyTapped = container.has<TappedComponent>()

            if (effect.tap) {
                // Only untapped permanents can be tapped (CR 701.21a); tapping an
                // already-tapped permanent is a no-op that emits no TappedEvent, so
                // "becomes tapped" triggers don't fire on a non-transition (CR 603.2f).
                if (alreadyTapped) continue
                currentState = currentState.updateEntity(entityId) { it.with(TappedComponent) }
                events.add(TappedEvent(entityId, cardName))
            } else {
                if (!alreadyTapped) continue
                currentState = currentState.updateEntity(entityId) { it.without<TappedComponent>() }
                events.add(UntappedEvent(entityId, cardName))
            }
        }

        return EffectResult.success(currentState, events)
    }
}
