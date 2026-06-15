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
    val playerOrder: List<String> = emptyList(),  // Player IDs in pack-passing order
    // Winston Draft-specific state
    val winstonMainDeckNames: List<String> = emptyList(),
    val winstonPileNames: List<List<String>> = emptyList(),  // 3 piles
    val winstonActivePlayerIndex: Int = 0,
    val winstonCurrentPileIndex: Int = 0,
    /** Card names each player has seen during Winston Draft: playerId -> list of card names */
    val winstonSeenCardNames: Map<String, List<String>> = emptyMap(),
    /** Epoch millis when tournament was marked complete, or null if still active */
    val completedAt: Long? = null,
    val isPublic: Boolean = false,
    /** Master switch for in-app AI assistance (Suggest Pick / Auto-build). */
    val aiAssistEnabled: Boolean = true,
    /** Lobby mode axis: LobbyGameMode enum name ("TOURNAMENT" / "FREE_FOR_ALL"). */
    val gameMode: String = "TOURNAMENT",
    /** FFA attack rule: AttackMode enum name ("MULTIPLE" / "LEFT" / "RIGHT"). */
    val attackMode: String = "MULTIPLE",
    /** 2HG: true = random teams each game, false = host-set teams via [teamAssignments]. */
    val randomTeams: Boolean = true,
    /** 2HG manual team assignment: playerId -> team index (0 or 1). Empty when unset/random. */
    val teamAssignments: Map<String, Int> = emptyMap(),
    /** FFA mode: session id of the game currently in progress, or null between games. */
    val ffaGameSessionId: String? = null,
    /** FFA mode: completed games in this lobby's play-again loop. */
    val ffaGamesPlayed: Int = 0
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
    val packQueueNames: List<List<String>> = emptyList(),  // Draft only: queued packs (async passing)
    val submittedDeck: Map<String, Int>?,  // cardName -> count
    val currentSpectatingGameId: String? = null,  // Game being spectated (for bye players)
    val isAi: Boolean = false,
    val aiModelOverride: String? = null
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
