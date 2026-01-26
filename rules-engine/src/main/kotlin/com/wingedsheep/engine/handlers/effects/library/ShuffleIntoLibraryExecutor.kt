package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LibraryShuffledEvent
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
import com.wingedsheep.sdk.scripting.ShuffleIntoLibraryEffect
import kotlin.reflect.KClass

/**
 * Executor for ShuffleIntoLibraryEffect.
 * "Shuffle [target] into its owner's library"
 *
 * Used by cards like Alabaster Dragon ("When Alabaster Dragon dies,
 * shuffle it into its owner's library.")
 */
class ShuffleIntoLibraryExecutor : EffectExecutor<ShuffleIntoLibraryEffect> {

    override val effectType: KClass<ShuffleIntoLibraryEffect> = ShuffleIntoLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: ShuffleIntoLibraryEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for shuffle into library")

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

        // Add to owner's library
        val libraryZone = ZoneKey(ownerId, ZoneType.LIBRARY)
        newState = newState.addToZone(libraryZone, targetId)

        events.add(
            ZoneChangeEvent(
                entityId = targetId,
                entityName = cardComponent.name,
                fromZone = currentZone.zoneType,
                toZone = ZoneType.LIBRARY,
                ownerId = ownerId
            )
        )

        // Shuffle the library
        val library = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to library))

        events.add(LibraryShuffledEvent(ownerId))

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
