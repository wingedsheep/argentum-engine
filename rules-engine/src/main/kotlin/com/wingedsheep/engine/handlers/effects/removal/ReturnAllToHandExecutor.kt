package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.cleanupCombatReferences
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.stripBattlefieldComponents
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ReturnAllToHandEffect
import kotlin.reflect.KClass

/**
 * Executor for ReturnAllToHandEffect.
 *
 * Returns all permanents matching the filter to their owners' hands.
 * Uses projected state for filter matching (type, color, keywords, etc.).
 *
 * @deprecated Use Effects.ReturnAllToHand() which decomposes into ForEachInGroup + MoveToZone pipeline.
 */
@Deprecated("Use Effects.ReturnAllToHand() or EffectPatterns.returnAllToHand() instead")
class ReturnAllToHandExecutor : EffectExecutor<ReturnAllToHandEffect> {

    override val effectType: KClass<ReturnAllToHandEffect> = ReturnAllToHandEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ReturnAllToHandEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
        val projected = state.projectedState
        val predicateContext = PredicateContext.fromEffectContext(context)

        // Find all matching permanents
        val matchedEntities = mutableListOf<EntityId>()
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue

            // Respect excludeSelf
            if (effect.filter.excludeSelf && entityId == sourceId) continue

            if (!predicateEvaluator.matchesWithProjection(
                    state, projected, entityId, effect.filter.baseFilter, predicateContext
                )
            ) continue

            matchedEntities.add(entityId)
        }

        if (matchedEntities.isEmpty()) {
            return ExecutionResult.success(state, emptyList())
        }

        // Return each matched permanent to its owner's hand
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in matchedEntities) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val ownerId = container.get<OwnerComponent>()?.playerId
                ?: cardComponent.ownerId
                ?: continue

            val currentZone = newState.zones.entries.find { (_, cards) -> entityId in cards }?.key
                ?: continue

            val handZone = ZoneKey(ownerId, Zone.HAND)

            // Clean up combat references and strip battlefield components
            newState = cleanupCombatReferences(newState, entityId)
            newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }
            newState = newState.removeFromZone(currentZone, entityId)
            newState = newState.addToZone(handZone, entityId)

            events.add(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.HAND,
                    ownerId = ownerId
                )
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
