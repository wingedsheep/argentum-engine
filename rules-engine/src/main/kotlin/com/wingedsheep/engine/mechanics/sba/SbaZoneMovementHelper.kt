package com.wingedsheep.engine.mechanics.sba

import com.wingedsheep.engine.core.CreatureDestroyedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.cleanupReverseAttachmentLink
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.stripBattlefieldComponents
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Consolidates zone movement logic for state-based actions.
 *
 * All SBAs that move permanents off the battlefield call this helper instead of
 * doing inline zone movement. This ensures the full cleanup pipeline runs:
 *
 * 1. Capture last-known information (counters, P/T, type line)
 * 2. Check ExileOnDeath floating effect replacement
 * 3. checkZoneChangeRedirect() for replacement effects
 * 4. cleanupReverseAttachmentLink()
 * 5. Remove from zone / add to zone
 * 6. cleanupCombatReferences()
 * 7. stripBattlefieldComponents()
 * 8. removeFloatingEffectsTargeting() (Rule 400.7 — the previously missing step)
 * 9. Apply additional replacement effects
 * 10. Handle ExileControllerGraveyardOnDeath if applicable
 * 11. Emit events
 */
object SbaZoneMovementHelper {

    /**
     * Move a creature to graveyard via SBA (zero toughness, lethal damage).
     * Emits both CreatureDestroyedEvent and ZoneChangeEvent.
     * Respects ExileOnDeath, zone change redirects, and ExileControllerGraveyardOnDeath.
     */
    fun putCreatureInGraveyard(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        reason: String
    ): ExecutionResult {
        val container = state.getEntity(entityId) ?: return ExecutionResult.success(state)
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.success(state)
        val ownerId = cardComponent.ownerId ?: controllerId

        // Capture last-known information before stripping
        val lastKnownCounterCount = container.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        val projected = state.projectedState
        val lastKnownPower = projected.getPower(entityId)
        val lastKnownToughness = projected.getToughness(entityId)

        // Check for ExileOnDeath replacement effect
        val exileOnDeathIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ExileOnDeath &&
                entityId in effect.effect.affectedEntities
        }
        val exileInstead = exileOnDeathIndex != -1

        // Check for RedirectZoneChange replacement effects
        val redirectResult = ZoneMovementUtils.checkZoneChangeRedirect(
            state, entityId, Zone.BATTLEFIELD, Zone.GRAVEYARD
        )
        val destinationZone = if (exileInstead) Zone.EXILE else redirectResult.destinationZone

        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val destinationZoneKey = ZoneKey(ownerId, destinationZone)

        var newState = state

        // Consume the ExileOnDeath floating effect if used
        if (exileInstead) {
            val updatedEffects = state.floatingEffects.toMutableList()
            updatedEffects.removeAt(exileOnDeathIndex)
            newState = newState.copy(floatingEffects = updatedEffects)
        }

        // Check for ExileControllerGraveyardOnDeath marker
        val exileGraveyardIndex = newState.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ExileControllerGraveyardOnDeath &&
                entityId in effect.effect.affectedEntities
        }
        val exileGraveyard = exileGraveyardIndex != -1

        // Consume the ExileControllerGraveyardOnDeath floating effect if present
        if (exileGraveyard) {
            val updatedEffects = newState.floatingEffects.toMutableList()
            val currentIndex = updatedEffects.indexOfFirst { effect ->
                effect.effect.modification is SerializableModification.ExileControllerGraveyardOnDeath &&
                    entityId in effect.effect.affectedEntities
            }
            if (currentIndex != -1) {
                updatedEffects.removeAt(currentIndex)
                newState = newState.copy(floatingEffects = updatedEffects)
            }
        }

        // Clean up reverse attachment link before moving
        newState = cleanupReverseAttachmentLink(newState, entityId)

        newState = newState.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(destinationZoneKey, entityId)

        // Clean up combat references before stripping components
        newState = ZoneMovementUtils.cleanupCombatReferences(newState, entityId)

        // Remove permanent components
        newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }

        // Remove floating effects targeting this entity (Rule 400.7)
        newState = ZoneMovementUtils.removeFloatingEffectsTargeting(newState, entityId)

        val events = mutableListOf<GameEvent>(
            CreatureDestroyedEvent(entityId, cardComponent.name, reason, controllerId),
            ZoneChangeEvent(
                entityId,
                cardComponent.name,
                Zone.BATTLEFIELD,
                destinationZone,
                ownerId,
                lastKnownCounterCount = lastKnownCounterCount,
                lastKnownPower = lastKnownPower,
                lastKnownToughness = lastKnownToughness,
                lastKnownTypeLine = cardComponent.typeLine
            )
        )

        // If ExileControllerGraveyardOnDeath was triggered, exile the controller's graveyard
        if (exileGraveyard) {
            val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
            val exileZone = ZoneKey(controllerId, Zone.EXILE)
            val graveyardCardIds = newState.getZone(graveyardZone).toList()
            for (cardId in graveyardCardIds) {
                val cardComp = newState.getEntity(cardId)?.get<CardComponent>()
                val cardOwnerId = cardComp?.ownerId ?: controllerId
                val ownerExileZone = ZoneKey(cardOwnerId, Zone.EXILE)
                newState = newState.removeFromZone(graveyardZone, cardId)
                newState = newState.addToZone(ownerExileZone, cardId)
                events.add(
                    ZoneChangeEvent(
                        cardId,
                        cardComp?.name ?: "Unknown",
                        Zone.GRAVEYARD,
                        Zone.EXILE,
                        cardOwnerId
                    )
                )
            }
        }

        // Apply additional replacement effect (e.g., Ugin's Nexus extra turn, Darigaaz egg counters)
        if (redirectResult.additionalEffect != null) {
            newState = ZoneMovementUtils.applyReplacementAdditionalEffect(
                newState, redirectResult.additionalEffect, redirectResult.effectControllerId, entityId
            )
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Move a permanent to graveyard via SBA (planeswalker loyalty, saga sacrifice,
     * unattached aura, legend rule). Emits ZoneChangeEvent only (no CreatureDestroyedEvent).
     * Respects zone change redirects.
     */
    fun putPermanentInGraveyard(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        lastKnownAttachedTo: EntityId? = null
    ): ExecutionResult {
        val container = state.getEntity(entityId) ?: return ExecutionResult.success(state)
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.success(state)
        val ownerId = cardComponent.ownerId ?: controllerId

        // Capture last-known information before stripping
        val lastKnownCounterCount = container.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        val projected = state.projectedState
        val lastKnownPower = projected.getPower(entityId)
        val lastKnownToughness = projected.getToughness(entityId)

        // Check for zone change replacement effects
        val redirectResult = ZoneMovementUtils.checkZoneChangeRedirect(
            state, entityId, Zone.BATTLEFIELD, Zone.GRAVEYARD
        )
        val destinationZone = redirectResult.destinationZone

        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val destinationZoneKey = ZoneKey(ownerId, destinationZone)

        var newState = state

        // Clean up reverse attachment link before moving
        newState = cleanupReverseAttachmentLink(newState, entityId)

        newState = newState.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(destinationZoneKey, entityId)

        // Clean up combat references
        newState = ZoneMovementUtils.cleanupCombatReferences(newState, entityId)

        // Remove permanent components
        newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }

        // Remove floating effects targeting this entity (Rule 400.7)
        newState = ZoneMovementUtils.removeFloatingEffectsTargeting(newState, entityId)

        val events = mutableListOf<GameEvent>(
            ZoneChangeEvent(
                entityId,
                cardComponent.name,
                Zone.BATTLEFIELD,
                destinationZone,
                ownerId,
                lastKnownCounterCount = lastKnownCounterCount,
                lastKnownPower = lastKnownPower,
                lastKnownToughness = lastKnownToughness,
                lastKnownTypeLine = cardComponent.typeLine,
                lastKnownAttachedTo = lastKnownAttachedTo
            )
        )

        // Apply additional replacement effect if any
        if (redirectResult.additionalEffect != null) {
            newState = ZoneMovementUtils.applyReplacementAdditionalEffect(
                newState, redirectResult.additionalEffect, redirectResult.effectControllerId, entityId
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
