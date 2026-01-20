package com.wingedsheep.gameserver.masking

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.PlayerComponent
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Masks game state to hide information from opponents.
 *
 * Masking rules follow MTG rules for hidden information:
 * - Hand: Only visible to owner
 * - Library: Hidden from all players (only size visible)
 * - Graveyard, Battlefield, Stack, Exile: Visible to all
 */
class StateMasker {

    /**
     * Create a masked view of the game state for a specific player.
     */
    fun mask(state: GameState, viewingPlayerId: EntityId): MaskedGameState {
        val playerIds = state.getPlayerIds()

        // Mask entities based on zone visibility
        val maskedEntities = mutableMapOf<EntityId, MaskedEntity>()
        val maskedZones = mutableMapOf<ZoneId, MaskedZone>()

        for ((zoneId, entityIds) in state.zones) {
            val isVisible = isZoneVisibleTo(zoneId, viewingPlayerId)
            val maskedZone = MaskedZone(
                zoneId = zoneId,
                entityIds = if (isVisible) entityIds else emptyList(),
                size = entityIds.size,
                isVisible = isVisible
            )
            maskedZones[zoneId] = maskedZone

            // Mask entities in this zone
            // Note: ComponentContainer is not sent to clients because Component polymorphic
            // serialization is not configured. Entity data should be sent via dedicated
            // serializable DTOs when needed by the client.
            for (entityId in entityIds) {
                maskedEntities[entityId] = MaskedEntity(
                    id = entityId,
                    isVisible = isVisible,
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
            currentPhase = state.currentPhase,
            currentStep = state.currentStep,
            activePlayerId = state.activePlayerId,
            priorityPlayerId = state.priorityPlayerId,
            turnNumber = state.turnNumber,
            isGameOver = state.isGameOver,
            winnerId = state.winner
        )
    }

    /**
     * Determine if a zone's contents are visible to a player.
     */
    private fun isZoneVisibleTo(zoneId: ZoneId, viewingPlayerId: EntityId): Boolean {
        return when (zoneId.type) {
            // Library is hidden from everyone
            ZoneType.LIBRARY -> false

            // Hand is only visible to its owner
            ZoneType.HAND -> zoneId.ownerId == viewingPlayerId

            // All other zones are public
            ZoneType.BATTLEFIELD,
            ZoneType.GRAVEYARD,
            ZoneType.STACK,
            ZoneType.EXILE,
            ZoneType.COMMAND -> true
        }
    }

    /**
     * Create a masked player representation with public info.
     */
    private fun createMaskedPlayer(state: GameState, playerId: EntityId): MaskedPlayer {
        val playerComponent = state.getComponent<PlayerComponent>(playerId)

        return MaskedPlayer(
            playerId = playerId,
            name = playerComponent?.name ?: "Unknown",
            life = state.getLife(playerId),
            poisonCounters = state.getPoisonCounters(playerId),
            handSize = state.getHandSize(playerId),
            librarySize = state.getLibrarySize(playerId),
            graveyardSize = state.getGraveyardSize(playerId),
            landsPlayedThisTurn = state.getLandsPlayed(playerId),
            hasLost = state.hasLost(playerId)
        )
    }
}
