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
    /**
     * Rules axis: [com.wingedsheep.sdk.core.GameRules] name, or **null for a row written before the
     * axis existed** — which is why this is nullable rather than defaulting to "STANDARD". With a
     * non-null default a legacy row would be indistinguishable from a host who explicitly chose
     * Standard, so restore would have to OR the two and a deliberate "Commander Draft, Standard
     * rules" lobby would silently flip back to Commander after a restart. Null means *infer*.
     *
     * Note this row still carries neither `deckFormat` nor `commanderPreset` — a pre-existing gap
     * (`backlog/menu-lobby-restructure-and-help.md:534`). Persisting `rules` closes the part of it
     * that decides whether a restored lobby plays Commander at all.
     */
    val rules: String? = null,
    val boosterCount: Int,
    val maxPlayers: Int,
    val pickTimeSeconds: Int = 45,
    val gamesPerMatch: Int,
    val state: String,  // LobbyState enum name
    val hostPlayerId: String?,
    val players: Map<String, PersistentLobbyPlayer>,  // playerId.value -> player state
    /** Cube definition plus the ordered undealt tail, so a restart cannot redeal drafted cards. */
    val cubeName: String? = null,
    val cubeCardNames: List<String> = emptyList(),
    val cubeBasicLandSetCode: String? = null,
    val cubePackSize: Int? = null,
    val cubeDealerRemainingCardNames: List<String> = emptyList(),
    /** Cube Pool Play (no draft, whole cube as everyone's pool). Meaningless without a cube. */
    val cubePoolPlay: Boolean = false,
    val bannedCardNames: Set<String> = emptySet(),
    val includedSetProducts: Map<String, Set<String>> = emptyMap(),
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
    val aiModelOverride: String? = null,
    val submittedSideboard: Map<String, Int> = emptyMap(),  // cardName -> count (outside the game)
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
    val submittedDeck: Map<String, Int>?,
    val submittedSideboard: Map<String, Int> = emptyMap()
)
