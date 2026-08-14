package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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
    ): EffectResult {
        val entityIds = context.pipeline.storedCollections[effect.collectionName]
            ?: return EffectResult.success(state)

        if (entityIds.isEmpty()) return EffectResult.success(state)

        val counterType = resolveCounterType(effect.counterType)

        // A dynamic [amount] overrides the static [count] — evaluated once at resolution.
        val baseCount = effect.amount?.let { DynamicAmountEvaluator().evaluate(state, it, context) }
            ?: effect.count
        if (baseCount <= 0) return EffectResult.success(state)

        var currentState = state
        val events = mutableListOf<CountersAddedEvent>()

        for (entityId in entityIds) {
            if (currentState.getEntity(entityId) == null) continue

            val current = currentState.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
            val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                currentState, entityId, counterType, baseCount, placerId = context.controllerId
            )

            val firstThisTurn = DamageUtils.isFirstCounterThisTurn(currentState, entityId)
            currentState = currentState.updateEntity(entityId) { container ->
                container.with(current.withAdded(counterType, modifiedCount))
            }
            currentState = DamageUtils.markCounterPlacedOnCreature(currentState, context.controllerId, entityId, counterTypeToString(counterType))

            val entityName = currentState.getEntity(entityId)?.get<CardComponent>()?.name ?: ""
            events.add(CountersAddedEvent(entityId, effect.counterType, modifiedCount, entityName, firstThisTurn, placedBy = context.controllerId))
        }

        return EffectResult.success(currentState, events)
    }
}
