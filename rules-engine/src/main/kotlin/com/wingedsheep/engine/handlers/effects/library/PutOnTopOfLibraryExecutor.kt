package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PutOnTopOfLibraryEffect
import kotlin.reflect.KClass

/**
 * Executor for PutOnTopOfLibraryEffect.
 * "Put [target] on top of its owner's library"
 *
 * Used by cards like Time Ebb ("Put target creature on top of its owner's library.")
 */
class PutOnTopOfLibraryExecutor : EffectExecutor<PutOnTopOfLibraryEffect> {

    override val effectType: KClass<PutOnTopOfLibraryEffect> = PutOnTopOfLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: PutOnTopOfLibraryEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for put on top of library")

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target entity not found: $targetId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card: $targetId")

        // Find the owner of the card
        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine card owner")

        // Find the card's current zone
        val currentZone = findEntityZone(state, targetId)
            ?: return ExecutionResult.error(state, "Card not found in any zone: $targetId")

        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        var newState = state

        // Remove from current zone
        newState = newState.removeFromZone(currentZone, targetId)

        // Add to TOP of owner's library (index 0)
        val libraryZone = ZoneKey(ownerId, ZoneType.LIBRARY)
        val currentLibrary = newState.getZone(libraryZone)
        val newLibrary = listOf(targetId) + currentLibrary
        newState = newState.copy(zones = newState.zones + (libraryZone to newLibrary))

        events.add(
            ZoneChangeEvent(
                entityId = targetId,
                entityName = cardComponent.name,
                fromZone = currentZone.zoneType,
                toZone = ZoneType.LIBRARY,
                ownerId = ownerId
            )
        )

        return ExecutionResult.success(newState, events)
    }

    /**
     * Find which zone an entity is currently in.
     */
    private fun findEntityZone(state: GameState, entityId: EntityId): ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
