package com.wingedsheep.gameserver.tournament

import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TournamentManager::class.java)

/**
 * Standing for a single player in the tournament.
 */
data class PlayerStanding(
    val playerId: EntityId,
    val playerName: String,
    var wins: Int = 0,
    var losses: Int = 0,
    var draws: Int = 0
) {
    val points: Int get() = wins * 3 + draws * 1
}

/**
 * A single match in a round.
 */
data class TournamentMatch(
    val player1Id: EntityId,
    val player2Id: EntityId?, // null = BYE
    var gameSessionId: String? = null,
    var winnerId: EntityId? = null,
    var isDraw: Boolean = false,
    var isComplete: Boolean = false
) {
    val isBye: Boolean get() = player2Id == null
}

/**
 * A round of matches in the tournament.
 */
data class TournamentRound(
    val roundNumber: Int,
    val matches: List<TournamentMatch>
) {
    val isComplete: Boolean get() = matches.all { it.isComplete }
}

/**
 * Manages a round-robin tournament for a sealed lobby.
 *
 * Uses the circle method for scheduling: N-1 rounds for N players,
 * floor(N/2) matches per round. Odd player counts get a BYE per round.
 */
class TournamentManager(
    private val lobbyId: String,
    players: List<Pair<EntityId, String>> // (playerId, playerName)
) {
    private val standings = players.associate { (id, name) ->
        id to PlayerStanding(id, name)
    }.toMutableMap()

    private val rounds: MutableList<TournamentRound> = mutableListOf()
    private var currentRoundIndex: Int = -1

    val totalRounds: Int
    val playerIds: List<EntityId> = players.map { it.first }

    init {
        val n = players.size
        totalRounds = if (n <= 1) 0 else n - 1 + (if (n % 2 != 0) 1 else 0)

        // Generate full round-robin schedule using circle method
        generateSchedule(players.map { it.first })
    }

    val currentRound: TournamentRound? get() =
        if (currentRoundIndex in rounds.indices) rounds[currentRoundIndex] else null

    val isComplete: Boolean get() = currentRoundIndex >= rounds.size - 1 &&
        (currentRound?.isComplete ?: true)

    /**
     * Generate round-robin schedule using the circle method.
     *
     * For N players (padded to even with a BYE sentinel):
     * - Fix player[0] in place
     * - Rotate remaining players through positions
     * - Each round pairs player[i] with player[N-1-i]
     */
    private fun generateSchedule(playerIds: List<EntityId>) {
        val ids = playerIds.toMutableList()
        val hasBye = ids.size % 2 != 0

        // Pad with null sentinel for BYE if odd number of players
        val paddedIds: MutableList<EntityId?> = ids.map<EntityId, EntityId?> { it }.toMutableList()
        if (hasBye) {
            paddedIds.add(null)
        }

        val n = paddedIds.size
        val numRounds = n - 1

        for (round in 0 until numRounds) {
            val matches = mutableListOf<TournamentMatch>()

            for (i in 0 until n / 2) {
                val p1 = paddedIds[i]
                val p2 = paddedIds[n - 1 - i]

                if (p1 != null) {
                    matches.add(TournamentMatch(
                        player1Id = p1,
                        player2Id = p2
                    ))
                } else if (p2 != null) {
                    // BYE is always player2 (null)
                    matches.add(TournamentMatch(
                        player1Id = p2,
                        player2Id = null
                    ))
                }
            }

            rounds.add(TournamentRound(roundNumber = round + 1, matches = matches))

            // Rotate: keep paddedIds[0] fixed, rotate the rest
            if (n > 2) {
                val last = paddedIds.removeAt(n - 1)
                paddedIds.add(1, last)
            }
        }

        logger.info("Generated $numRounds rounds for ${playerIds.size} players in lobby $lobbyId")
    }

    /**
     * Advance to the next round. Returns the round, or null if tournament is complete.
     */
    fun startNextRound(): TournamentRound? {
        currentRoundIndex++
        if (currentRoundIndex >= rounds.size) {
            return null
        }

        val round = rounds[currentRoundIndex]

        // Auto-complete BYE matches
        for (match in round.matches) {
            if (match.isBye) {
                match.winnerId = match.player1Id
                match.isComplete = true
                standings[match.player1Id]?.wins = (standings[match.player1Id]?.wins ?: 0) + 1
                logger.info("BYE for ${standings[match.player1Id]?.playerName} in round ${round.roundNumber}")
            }
        }

        return round
    }

    /**
     * Record a match result.
     */
    fun reportMatchResult(gameSessionId: String, winnerId: EntityId?) {
        val round = currentRound ?: return
        val match = round.matches.find { it.gameSessionId == gameSessionId } ?: return

        if (match.isComplete) return

        match.isComplete = true

        if (winnerId != null) {
            match.winnerId = winnerId
            standings[winnerId]?.wins = (standings[winnerId]?.wins ?: 0) + 1

            val loserId = if (match.player1Id == winnerId) match.player2Id else match.player1Id
            if (loserId != null) {
                standings[loserId]?.losses = (standings[loserId]?.losses ?: 0) + 1
            }
        } else {
            match.isDraw = true
            standings[match.player1Id]?.draws = (standings[match.player1Id]?.draws ?: 0) + 1
            match.player2Id?.let { p2 ->
                standings[p2]?.draws = (standings[p2]?.draws ?: 0) + 1
            }
        }

        logger.info("Match result reported for game $gameSessionId: winner=${winnerId?.value ?: "draw"}")
    }

    /**
     * Record an auto-loss for a player who abandoned the tournament.
     */
    fun recordAbandon(playerId: EntityId) {
        // Record losses for all remaining matches
        for (round in rounds) {
            for (match in round.matches) {
                if (match.isComplete) continue
                if (match.player1Id == playerId || match.player2Id == playerId) {
                    match.isComplete = true
                    val opponentId = if (match.player1Id == playerId) match.player2Id else match.player1Id
                    if (opponentId != null) {
                        match.winnerId = opponentId
                        standings[opponentId]?.wins = (standings[opponentId]?.wins ?: 0) + 1
                    }
                    standings[playerId]?.losses = (standings[playerId]?.losses ?: 0) + 1
                }
            }
        }
    }

    /**
     * Check if the current round is complete.
     */
    fun isRoundComplete(): Boolean = currentRound?.isComplete ?: true

    /**
     * Get current standings sorted by points (descending).
     */
    fun getStandings(): List<PlayerStanding> {
        return standings.values.sortedByDescending { it.points }
    }

    /**
     * Get standings as server message format.
     */
    fun getStandingsInfo(connectedPlayerIds: Set<EntityId> = emptySet()): List<ServerMessage.PlayerStandingInfo> {
        return getStandings().map { s ->
            ServerMessage.PlayerStandingInfo(
                playerId = s.playerId.value,
                playerName = s.playerName,
                wins = s.wins,
                losses = s.losses,
                draws = s.draws,
                points = s.points,
                isConnected = connectedPlayerIds.isEmpty() || s.playerId in connectedPlayerIds
            )
        }
    }

    /**
     * Get match results for the current round.
     */
    fun getCurrentRoundResults(): List<ServerMessage.MatchResultInfo> {
        val round = currentRound ?: return emptyList()
        return round.matches.map { match ->
            ServerMessage.MatchResultInfo(
                player1Name = standings[match.player1Id]?.playerName ?: "Unknown",
                player2Name = match.player2Id?.let { standings[it]?.playerName } ?: "BYE",
                winnerId = match.winnerId?.value,
                isDraw = match.isDraw,
                isBye = match.isBye
            )
        }
    }

    /**
     * Get the non-BYE matches for the current round that need game sessions.
     */
    fun getCurrentRoundGameMatches(): List<TournamentMatch> {
        val round = currentRound ?: return emptyList()
        return round.matches.filter { !it.isBye && !it.isComplete }
    }
}
