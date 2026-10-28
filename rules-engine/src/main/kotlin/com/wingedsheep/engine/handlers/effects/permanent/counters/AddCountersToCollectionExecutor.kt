package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import kotlin.reflect.KClass

/**
 * Executor for AddCountersToCollectionEffect.
 * Adds counters to each entity in a named collection.
 */
class AddCountersToCollectionExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<AddCountersToCollectionEffect> {

    private val placement = AddCountersExecutor(amountEvaluator.predicates)

    override val effectType: KClass<AddCountersToCollectionEffect> = AddCountersToCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: AddCountersToCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val entityIds = context.pipeline.storedCollections[effect.collectionName]
            ?: return EffectResult.success(state)

        if (entityIds.isEmpty()) return EffectResult.success(state)

        // A dynamic [amount] overrides the static [count] — evaluated once at resolution.
        val baseCount = effect.amount?.let { amountEvaluator.evaluate(state, it, context) }
            ?: effect.count
        if (baseCount <= 0) return EffectResult.success(state)

        var currentState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in entityIds) {
            if (currentState.getEntity(entityId) == null) continue

            // Reuse prohibition, replacement/cap, event and placement-history handling.
            val result = placement.placeOn(currentState, entityId, effect.counterType, baseCount, context)
            currentState = result.state
            events.addAll(result.events)
        }

        return EffectResult.success(currentState, events)
    }
}
