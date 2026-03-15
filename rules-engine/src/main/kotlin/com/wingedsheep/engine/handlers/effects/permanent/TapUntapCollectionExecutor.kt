package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
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
    ): ExecutionResult {
        val entityIds = context.storedCollections[effect.collectionName]
            ?: return ExecutionResult.success(state)

        if (entityIds.isEmpty()) return ExecutionResult.success(state)

        var currentState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in entityIds) {
            val container = currentState.getEntity(entityId) ?: continue
            val cardName = container.get<CardComponent>()?.name ?: "Permanent"

            currentState = currentState.updateEntity(entityId) { c ->
                if (effect.tap) {
                    c.with(TappedComponent)
                } else {
                    c.without<TappedComponent>()
                }
            }

            events.add(
                if (effect.tap) TappedEvent(entityId, cardName)
                else UntappedEvent(entityId, cardName)
            )
        }

        return ExecutionResult.success(currentState, events)
    }
}
