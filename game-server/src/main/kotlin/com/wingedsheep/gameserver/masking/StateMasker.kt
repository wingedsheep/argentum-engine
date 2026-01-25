package com.wingedsheep.gameserver.masking

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent

/**
 * Masks game state to hide information from opponents.
 *
 * Masking rules follow MTG rules for hidden information:
 * - Hand: Only visible to owner (unless cards have been revealed via "look at hand" effects)
 * - Library: Hidden from all players (only size visible)
 * - Graveyard, Battlefield, Stack, Exile: Visible to all
 */
class StateMasker {

    /**
     * Create a masked view of the game state for a specific player.
     */
    fun mask(state: GameState, viewingPlayerId: EntityId): MaskedGameState {
        val playerIds = state.turnOrder

        // Mask entities based on zone visibility
        // Only include visible entities in the entities map - hidden entities are
        // represented only by zone sizes (MaskedZone.size) to reduce message size.
        val maskedEntities = mutableMapOf<EntityId, MaskedEntity>()
        val maskedZones = mutableListOf<MaskedZone>()

        for ((zoneKey, entityIds) in state.zones) {
            val isZoneVisible = isZoneVisibleTo(zoneKey, viewingPlayerId)

            // For hidden zones (opponent's hand), check if individual cards are revealed
            val visibleEntityIds = if (isZoneVisible) {
                entityIds
            } else {
                // Check each entity for RevealedToComponent
                entityIds.filter { entityId ->
                    isEntityRevealedTo(state, entityId, viewingPlayerId)
                }
            }

            val maskedZone = MaskedZone(
                zoneKey = zoneKey,
                entityIds = visibleEntityIds,
                size = entityIds.size,
                isVisible = isZoneVisible || visibleEntityIds.isNotEmpty()
            )
            maskedZones.add(maskedZone)

            // Include visible entities in the entities map
            for (entityId in visibleEntityIds) {
                maskedEntities[entityId] = MaskedEntity(
                    id = entityId,
                    isVisible = true,
                    components = null  // Components not serialized to client
                )
            }
        }

        // Also include player entities (always visible)
        for (playerId in playerIds) {
            if (playerId !in maskedEntities) {
                maskedEntities[playerId] = MaskedEntity(
                    id = playerId,
                    isVisible = true,
                    components = null  // Components not serialized to client
                )
            }
        }

        // Create player info
        val maskedPlayers = playerIds.map { playerId ->
            createMaskedPlayer(state, playerId)
        }

        return MaskedGameState(
            viewingPlayerId = viewingPlayerId,
            entities = maskedEntities,
            zones = maskedZones,
            players = maskedPlayers,
            currentPhase = state.phase,
            currentStep = state.step,
            activePlayerId = state.activePlayerId,
            priorityPlayerId = state.priorityPlayerId,
            turnNumber = state.turnNumber,
            isGameOver = state.gameOver,
            winnerId = state.winnerId
        )
    }

    /**
     * Determine if a zone's contents are visible to a player.
     */
    private fun isZoneVisibleTo(zoneKey: ZoneKey, viewingPlayerId: EntityId): Boolean {
        return when (zoneKey.zoneType) {
            // Library is hidden from everyone
            ZoneType.LIBRARY -> false

            // Hand is only visible to its owner
            ZoneType.HAND -> zoneKey.ownerId == viewingPlayerId

            // All other zones are public
            ZoneType.BATTLEFIELD,
            ZoneType.GRAVEYARD,
            ZoneType.STACK,
            ZoneType.EXILE,
            ZoneType.COMMAND -> true
        }
    }

    /**
     * Check if an entity has been revealed to a specific player.
     * This is used for "look at hand" effects where the viewing player
     * can see cards that have been revealed to them.
     */
    private fun isEntityRevealedTo(state: GameState, entityId: EntityId, viewingPlayerId: EntityId): Boolean {
        val revealedComponent = state.getEntity(entityId)?.get<RevealedToComponent>()
        return revealedComponent?.isRevealedTo(viewingPlayerId) == true
    }

    /**
     * Create a masked player representation with public info.
     */
    private fun createMaskedPlayer(state: GameState, playerId: EntityId): MaskedPlayer {
        val container = state.getEntity(playerId)
        val playerComponent = container?.get<PlayerComponent>()
        val lifeTotalComponent = container?.get<LifeTotalComponent>()
        val landDropsComponent = container?.get<LandDropsComponent>()

        // Calculate zone sizes
        val handSize = state.getHand(playerId).size
        val librarySize = state.getLibrary(playerId).size
        val graveyardSize = state.getGraveyard(playerId).size

        // Determine lands played this turn
        val landsPlayed = if (landDropsComponent != null) {
            landDropsComponent.maxPerTurn - landDropsComponent.remaining
        } else {
            0
        }

        // Check if player has lost (they're not the winner and game is over)
        val hasLost = state.gameOver && state.winnerId != null && state.winnerId != playerId

        return MaskedPlayer(
            playerId = playerId,
            name = playerComponent?.name ?: "Unknown",
            life = lifeTotalComponent?.life ?: 20,
            poisonCounters = 0, // TODO: Add poison counter component support
            handSize = handSize,
            librarySize = librarySize,
            graveyardSize = graveyardSize,
            landsPlayedThisTurn = landsPlayed,
            hasLost = hasLost
        )
    }
}
