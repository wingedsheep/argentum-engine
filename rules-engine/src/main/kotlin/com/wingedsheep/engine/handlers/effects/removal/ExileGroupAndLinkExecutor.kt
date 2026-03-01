package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.stripBattlefieldComponents
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ExileGroupAndLinkEffect
import kotlin.reflect.KClass

/**
 * Executor for ExileGroupAndLinkEffect.
 *
 * Exiles all permanents matching the filter that the controller controls,
 * stores their entity IDs on the source permanent as a LinkedExileComponent,
 * and stores the IDs as a named collection in updatedCollections (so the
 * count can be referenced via DynamicAmount.VariableReference("{storeAs}_count")).
 */
class ExileGroupAndLinkExecutor : EffectExecutor<ExileGroupAndLinkEffect> {

    override val effectType: KClass<ExileGroupAndLinkEffect> = ExileGroupAndLinkEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ExileGroupAndLinkEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val sourceId = context.sourceId

        // Resolve matching permanents using projected state
        val projected = StateProjector().project(state)
        val predicateContext = PredicateContext.fromEffectContext(context)

        val matchedEntities = mutableListOf<EntityId>()
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue

            // Don't exile the source permanent itself
            if (entityId == sourceId) continue

            if (!predicateEvaluator.matchesWithProjection(
                    state, projected, entityId, effect.filter.baseFilter, predicateContext
                )
            ) continue

            matchedEntities.add(entityId)
        }

        if (matchedEntities.isEmpty()) {
            return ExecutionResult(
                state = state,
                updatedCollections = mapOf(effect.storeAs to emptyList())
            )
        }

        // Exile each matched permanent
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val exiledIds = mutableListOf<EntityId>()

        for (entityId in matchedEntities) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val ownerId = container.get<OwnerComponent>()?.playerId
                ?: cardComponent.ownerId
                ?: controllerId

            // Find the zone the permanent is currently in
            val currentZone = newState.zones.entries.find { (_, cards) -> entityId in cards }?.key
                ?: continue

            val exileZone = ZoneKey(ownerId, Zone.EXILE)

            // Strip battlefield components and move to exile
            newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }
            newState = newState.removeFromZone(currentZone, entityId)
            newState = newState.addToZone(exileZone, entityId)

            events.add(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.EXILE,
                    ownerId = ownerId
                )
            )

            exiledIds.add(entityId)
        }

        // Store exiled IDs on the source permanent as a LinkedExileComponent
        if (sourceId != null) {
            val sourceContainer = newState.getEntity(sourceId)
            if (sourceContainer != null) {
                val existingLinked = sourceContainer.get<LinkedExileComponent>()
                val allExiled = (existingLinked?.exiledIds ?: emptyList()) + exiledIds
                val allExiledCopy = allExiled
                newState = newState.updateEntity(sourceId) { c -> c.with(LinkedExileComponent(allExiledCopy)) }
            }
        }

        return ExecutionResult(
            state = newState,
            events = events,
            updatedCollections = mapOf(effect.storeAs to exiledIds.toList())
        )
    }
}
