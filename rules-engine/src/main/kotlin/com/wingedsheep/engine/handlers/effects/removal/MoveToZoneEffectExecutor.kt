package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.moveCardToZone
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.ZonePlacement
import kotlin.reflect.KClass

/**
 * Executor for MoveToZoneEffect.
 * Unified zone-moving effect that consolidates destroy, exile, bounce,
 * shuffle-into-library, put-on-top, etc.
 */
class MoveToZoneEffectExecutor : EffectExecutor<MoveToZoneEffect> {

    override val effectType: KClass<MoveToZoneEffect> = MoveToZoneEffect::class

    override fun execute(
        state: GameState,
        effect: MoveToZoneEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for move to zone")

        // byDestruction delegates to destroyPermanent (handles indestructible)
        if (effect.byDestruction) {
            return destroyPermanent(state, targetId)
        }

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target entity not found: $targetId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card: $targetId")

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine card owner")

        val currentZone = findEntityZone(state, targetId)
            ?: return ExecutionResult.error(state, "Card not found in any zone: $targetId")

        return when (effect.placement) {
            ZonePlacement.Top -> moveToLibraryTop(state, targetId, cardComponent, ownerId, currentZone)
            ZonePlacement.Bottom -> moveToLibraryBottom(state, targetId, cardComponent, ownerId, currentZone)
            ZonePlacement.Shuffled -> moveToLibraryShuffled(state, targetId, cardComponent, ownerId, currentZone)
            ZonePlacement.Tapped -> moveToBattlefieldTapped(state, targetId, cardComponent, ownerId, currentZone)
            ZonePlacement.Default -> moveCardToZone(state, targetId, effect.destination)
        }
    }

    private fun moveToLibraryTop(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val currentLibrary = newState.getZone(libraryZone)
        val newLibrary = listOf(entityId) + currentLibrary
        newState = newState.copy(zones = newState.zones + (libraryZone to newLibrary))

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.LIBRARY,
                    ownerId = ownerId
                )
            )
        )
    }

    private fun moveToLibraryBottom(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val currentLibrary = newState.getZone(libraryZone)
        val newLibrary = currentLibrary + entityId
        newState = newState.copy(zones = newState.zones + (libraryZone to newLibrary))

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.LIBRARY,
                    ownerId = ownerId
                )
            )
        )
    }

    private fun moveToLibraryShuffled(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        newState = newState.addToZone(libraryZone, entityId)

        // Shuffle the library
        val library = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to library))

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        val events = listOf<GameEvent>(
            ZoneChangeEvent(
                entityId = entityId,
                entityName = cardComponent.name,
                fromZone = currentZone.zoneType,
                toZone = Zone.LIBRARY,
                ownerId = ownerId
            ),
            LibraryShuffledEvent(ownerId)
        )

        return ExecutionResult.success(newState, events)
    }

    private fun moveToBattlefieldTapped(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val battlefieldZone = ZoneKey(ownerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, entityId)

        newState = newState.updateEntity(entityId) { c ->
            c.with(ControllerComponent(ownerId))
                .with(TappedComponent)
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = ownerId
                )
            )
        )
    }

    /**
     * Remove battlefield-specific components when leaving the battlefield.
     */
    private fun cleanupBattlefieldComponents(
        state: GameState,
        entityId: EntityId,
        fromZone: ZoneKey
    ): GameState {
        if (fromZone.zoneType != Zone.BATTLEFIELD) return state
        return state.updateEntity(entityId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
                .without<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()
        }
    }

    private fun findEntityZone(state: GameState, entityId: EntityId): ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
