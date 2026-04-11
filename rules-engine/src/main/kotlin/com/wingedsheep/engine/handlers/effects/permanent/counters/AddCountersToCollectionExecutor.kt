package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import kotlin.reflect.KClass

/**
 * Executor for AddCountersToCollectionEffect.
 * Adds counters to each entity in a named collection.
 */
class AddCountersToCollectionExecutor : EffectExecutor<AddCountersToCollectionEffect> {

    override val effectType: KClass<AddCountersToCollectionEffect> = AddCountersToCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: AddCountersToCollectionEffect,
        context: EffectContext
    ): ExecutionResult {
        val entityIds = context.pipeline.storedCollections[effect.collectionName]
            ?: return ExecutionResult.success(state)

        if (entityIds.isEmpty()) return ExecutionResult.success(state)

        val counterType = try {
            CounterType.valueOf(
                effect.counterType.uppercase()
                    .replace(' ', '_')
                    .replace('+', 'P')
                    .replace('-', 'M')
                    .replace("/", "_")
            )
        } catch (e: IllegalArgumentException) {
            CounterType.PLUS_ONE_PLUS_ONE
        }

        var currentState = state
        val events = mutableListOf<CountersAddedEvent>()

        for (entityId in entityIds) {
            if (currentState.getEntity(entityId) == null) continue

            val current = currentState.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
            val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                currentState, entityId, counterType, effect.count
            )

            currentState = currentState.updateEntity(entityId) { container ->
                container.with(current.withAdded(counterType, modifiedCount))
            }

            val entityName = currentState.getEntity(entityId)?.get<CardComponent>()?.name ?: ""
            events.add(CountersAddedEvent(entityId, effect.counterType, modifiedCount, entityName))
        }

        return ExecutionResult.success(currentState, events)
    }
}
