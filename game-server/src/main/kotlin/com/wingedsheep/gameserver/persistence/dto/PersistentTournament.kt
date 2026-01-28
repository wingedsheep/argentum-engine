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
    val playerIds: List<String>  // Ordered list of player IDs
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
    val draws: Int
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
    val isComplete: Boolean
)
