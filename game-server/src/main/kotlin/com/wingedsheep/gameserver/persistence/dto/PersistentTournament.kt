package com.wingedsheep.gameserver.persistence.dto

import kotlinx.serialization.Serializable

/**
 * Persistent representation of a TournamentManager for Redis storage.
 */
@Serializable
data class PersistentTournament(
    val lobbyId: String,
    val standings: Map<String, PersistentStanding>,  // playerId.value -> standing
    val rounds: List<PersistentRound>,
    val currentRoundIndex: Int,
    val totalRounds: Int,
    val gamesPerMatch: Int,
    val playerIds: List<String>,  // Ordered list of player IDs
    /** Games per match (1 or a best-of-N). Defaulted so pre-best-of-N payloads still decode. */
    val bestOf: Int = 1,
)

/**
 * Persistent player standing.
 */
@Serializable
data class PersistentStanding(
    val playerId: String,
    val playerName: String,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val gamesDrawn: Int = 0,
    val lifeDifferential: Int = 0,
    val forfeitWins: Int = 0,
    val forfeitLosses: Int = 0,
)

/**
 * Persistent tournament round.
 */
@Serializable
data class PersistentRound(
    val roundNumber: Int,
    val matches: List<PersistentMatch>
)

/**
 * Persistent tournament match.
 */
@Serializable
data class PersistentMatch(
    val player1Id: String,
    val player2Id: String?,  // null = BYE
    val gameSessionId: String?,
    val winnerId: String?,
    val isDraw: Boolean,
    val isComplete: Boolean,
    val player1GameWins: Int = 0,
    val player2GameWins: Int = 0,
    /** Defaulted so a payload written before simulated matches existed still decodes. */
    val isSimulated: Boolean = false,
    // Best-of-N state, all defaulted so a payload written before best-of-N decodes as a one-game match.
    val gamesDrawn: Int = 0,
    val completedGameSessionIds: List<String> = emptyList(),
    val playDrawChooserId: String? = null,
    val lastGameWinnerId: String? = null,
    val currentStarterId: String? = null,
    val nextStarterId: String? = null,
    /** playerId → card name → count: each seat's sideboarded deck for the rest of the match. */
    val matchDecks: Map<String, Map<String, Int>> = emptyMap(),
    val sideboardReady: List<String> = emptyList(),
)
