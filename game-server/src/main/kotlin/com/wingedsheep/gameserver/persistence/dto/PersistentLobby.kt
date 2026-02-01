package com.wingedsheep.gameserver.persistence.dto

import kotlinx.serialization.Serializable

/**
 * Persistent representation of a TournamentLobby for Redis storage.
 */
@Serializable
data class PersistentTournamentLobby(
    val lobbyId: String,
    val setCodes: List<String> = emptyList(),
    val setNames: List<String> = emptyList(),
    val format: String = "SEALED",  // TournamentFormat enum name
    val boosterCount: Int,
    val maxPlayers: Int,
    val pickTimeSeconds: Int = 45,
    val gamesPerMatch: Int,
    val state: String,  // LobbyState enum name
    val hostPlayerId: String?,
    val players: Map<String, PersistentLobbyPlayer>,  // playerId.value -> player state
    // Draft-specific state
    val currentPackNumber: Int = 0,
    val currentPickNumber: Int = 0,
    val playerOrder: List<String> = emptyList()  // Player IDs in pack-passing order
)

/**
 * Persistent player state within a lobby.
 * Card pool is stored as names only - CardDefinitions are regenerated from CardRegistry on load.
 */
@Serializable
data class PersistentLobbyPlayer(
    val playerId: String,
    val playerName: String,
    val token: String,
    val cardPoolNames: List<String>,  // Card names only
    val currentPackNames: List<String>? = null,  // Draft only: current pack cards
    val hasPicked: Boolean = false,  // Draft only
    val submittedDeck: Map<String, Int>?  // cardName -> count
)

/**
 * Persistent representation of a SealedSession (2-player sealed, legacy format).
 */
@Serializable
data class PersistentSealedSession(
    val sessionId: String,
    val setCodes: List<String>,
    val setNames: List<String>,
    val state: String,  // SealedSessionState enum name
    val players: Map<String, PersistentSealedPlayer>  // playerId.value -> player state
)

/**
 * Persistent player state within a 2-player sealed session.
 */
@Serializable
data class PersistentSealedPlayer(
    val playerId: String,
    val playerName: String,
    val cardPoolNames: List<String>,
    val submittedDeck: Map<String, Int>?
)
