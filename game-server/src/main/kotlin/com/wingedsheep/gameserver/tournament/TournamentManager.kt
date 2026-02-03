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
    var draws: Int = 0,
    var gamesWon: Int = 0,
    var gamesLost: Int = 0,
    var lifeDifferential: Int = 0
) {
    val points: Int get() = wins * 3 + draws * 1
}

/**
 * Reason why a player's position was determined by a tiebreaker.
 */
enum class TiebreakerReason {
    NONE,           // No tie - won on points
    HEAD_TO_HEAD,   // Won head-to-head matchup
    H2H_GAMES,      // Won on head-to-head game differential
    LIFE_DIFF,      // Won on life differential
    TIED            // True tie - shared position
}

/**
 * A player standing with calculated rank and tiebreaker information.
 */
data class RankedStanding(
    val standing: PlayerStanding,
    val rank: Int,
    val tiebreakerReason: TiebreakerReason
)

/**
 * A single match in a round.
 */
data class TournamentMatch(
    val player1Id: EntityId,
    val player2Id: EntityId?, // null = BYE
    var gameSessionId: String? = null,
    var winnerId: EntityId? = null,
    var isDraw: Boolean = false,
    var isComplete: Boolean = false,
    var player1GameWins: Int = 0,
    var player2GameWins: Int = 0
) {
    val isBye: Boolean get() = player2Id == null

    /**
     * Check if this match involves both of the specified players.
     */
    fun hasPlayers(p1: EntityId, p2: EntityId): Boolean {
        return (player1Id == p1 && player2Id == p2) || (player1Id == p2 && player2Id == p1)
    }
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
    players: List<Pair<EntityId, String>>, // (playerId, playerName)
    private val gamesPerMatch: Int = 1
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
        val baseRounds = if (n <= 1) 0 else n - 1 + (if (n % 2 != 0) 1 else 0)
        totalRounds = baseRounds * gamesPerMatch

        // Generate full round-robin schedule, repeated gamesPerMatch times
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
        val numBaseRounds = n - 1

        // Generate the base round-robin, then repeat it gamesPerMatch times
        var roundNumber = 0
        for (repetition in 0 until gamesPerMatch) {
            // Reset rotation for each repetition
            val rotatedIds: MutableList<EntityId?> = ids.map<EntityId, EntityId?> { it }.toMutableList()
            if (hasBye) rotatedIds.add(null)

            for (round in 0 until numBaseRounds) {
                roundNumber++
                val matches = mutableListOf<TournamentMatch>()

                for (i in 0 until n / 2) {
                    val p1 = rotatedIds[i]
                    val p2 = rotatedIds[n - 1 - i]

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

                rounds.add(TournamentRound(roundNumber = roundNumber, matches = matches))

                // Rotate: keep rotatedIds[0] fixed, rotate the rest
                if (n > 2) {
                    val last = rotatedIds.removeAt(n - 1)
                    rotatedIds.add(1, last)
                }
            }
        }

        logger.info("Generated $roundNumber rounds ($gamesPerMatch game(s) per match) for ${playerIds.size} players in lobby $lobbyId")
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

        // Auto-complete BYE matches (no points awarded for byes)
        for (match in round.matches) {
            if (match.isBye) {
                match.isComplete = true
                // Don't set winnerId or add wins - byes don't count for points
                logger.info("BYE for ${standings[match.player1Id]?.playerName} in round ${round.roundNumber} (no points)")
            }
        }

        return round
    }

    /**
     * Record a match result.
     *
     * @param gameSessionId The game session ID
     * @param winnerId The winner's player ID, or null for a draw
     * @param winnerLifeRemaining The winner's remaining life total (for tiebreaker calculations)
     */
    fun reportMatchResult(gameSessionId: String, winnerId: EntityId?, winnerLifeRemaining: Int = 0) {
        val round = currentRound ?: return
        val match = round.matches.find { it.gameSessionId == gameSessionId } ?: return

        if (match.isComplete) return

        match.isComplete = true

        if (winnerId != null) {
            match.winnerId = winnerId
            standings[winnerId]?.apply {
                wins += 1
                gamesWon += 1
                lifeDifferential += winnerLifeRemaining
            }

            val loserId = if (match.player1Id == winnerId) match.player2Id else match.player1Id
            if (loserId != null) {
                standings[loserId]?.apply {
                    losses += 1
                    gamesLost += 1
                }
            }

            // Track game wins per player in the match
            if (match.player1Id == winnerId) {
                match.player1GameWins += 1
            } else {
                match.player2GameWins += 1
            }
        } else {
            match.isDraw = true
            standings[match.player1Id]?.draws = (standings[match.player1Id]?.draws ?: 0) + 1
            match.player2Id?.let { p2 ->
                standings[p2]?.draws = (standings[p2]?.draws ?: 0) + 1
            }
        }

        logger.info("Match result reported for game $gameSessionId: winner=${winnerId?.value ?: "draw"}, life=$winnerLifeRemaining")
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

    // =========================================================================
    // Tiebreaker Functions
    // =========================================================================

    /**
     * Get the head-to-head result between two players.
     *
     * @return positive if player1 won more head-to-head matches,
     *         negative if player2 won more,
     *         0 if tied
     */
    private fun getHeadToHeadResult(player1: EntityId, player2: EntityId): Int {
        val matches = rounds.flatMap { it.matches }
            .filter { it.hasPlayers(player1, player2) && it.isComplete && !it.isBye }

        val p1Wins = matches.count { it.winnerId == player1 }
        val p2Wins = matches.count { it.winnerId == player2 }

        return p1Wins.compareTo(p2Wins)
    }

    /**
     * Get the head-to-head game differential between two players.
     * This counts individual game wins across all head-to-head matches.
     *
     * @return positive if player1 won more games in H2H matches,
     *         negative if player2 won more,
     *         0 if tied
     */
    private fun getHeadToHeadGameDiff(player1: EntityId, player2: EntityId): Int {
        val matches = rounds.flatMap { it.matches }
            .filter { it.hasPlayers(player1, player2) && it.isComplete && !it.isBye }

        var p1GameWins = 0
        var p2GameWins = 0

        for (match in matches) {
            if (match.player1Id == player1) {
                p1GameWins += match.player1GameWins
                p2GameWins += match.player2GameWins
            } else {
                // player1 is in the player2Id slot
                p1GameWins += match.player2GameWins
                p2GameWins += match.player1GameWins
            }
        }

        return p1GameWins.compareTo(p2GameWins)
    }

    /**
     * Determine which tiebreaker separated two players.
     *
     * @param higher The player ranked higher
     * @param lower The player ranked lower
     * @return The tiebreaker reason for the lower player
     */
    private fun determineTiebreakerUsed(higher: PlayerStanding, lower: PlayerStanding): TiebreakerReason {
        // If points differ, no tiebreaker needed
        if (higher.points != lower.points) {
            return TiebreakerReason.NONE
        }

        // Check head-to-head
        val h2hResult = getHeadToHeadResult(higher.playerId, lower.playerId)
        if (h2hResult > 0) {
            return TiebreakerReason.HEAD_TO_HEAD
        }

        // Check head-to-head game differential
        val h2hGameDiff = getHeadToHeadGameDiff(higher.playerId, lower.playerId)
        if (h2hGameDiff > 0) {
            return TiebreakerReason.H2H_GAMES
        }

        // Check life differential
        if (higher.lifeDifferential > lower.lifeDifferential) {
            return TiebreakerReason.LIFE_DIFF
        }

        // True tie
        return TiebreakerReason.TIED
    }

    /**
     * Get current standings sorted by points with tiebreakers applied.
     *
     * Tiebreaker order:
     * 1. Points (wins × 3 + draws)
     * 2. Head-to-head result
     * 3. Head-to-head game differential
     * 4. Life differential
     * 5. Tied (shared position)
     */
    fun getStandings(): List<PlayerStanding> {
        return standings.values.sortedWith(
            compareByDescending<PlayerStanding> { it.points }
                .thenComparator { a, b -> -getHeadToHeadResult(a.playerId, b.playerId) }
                .thenComparator { a, b -> -getHeadToHeadGameDiff(a.playerId, b.playerId) }
                .thenByDescending { it.lifeDifferential }
        )
    }

    /**
     * Get ranked standings with tiebreaker information.
     * Players who are truly tied share the same rank.
     */
    fun getRankedStandings(): List<RankedStanding> {
        val sorted = getStandings()
        if (sorted.isEmpty()) return emptyList()

        val result = mutableListOf<RankedStanding>()
        var currentRank = 1

        for (i in sorted.indices) {
            val standing = sorted[i]
            val tiebreakerReason: TiebreakerReason

            if (i == 0) {
                // First player - no tiebreaker needed
                tiebreakerReason = TiebreakerReason.NONE
            } else {
                val previous = sorted[i - 1]
                tiebreakerReason = determineTiebreakerUsed(previous, standing)

                // Update rank only if truly different (not TIED)
                if (tiebreakerReason != TiebreakerReason.TIED) {
                    currentRank = i + 1
                }
            }

            result.add(RankedStanding(standing, currentRank, tiebreakerReason))
        }

        return result
    }

    /**
     * Get standings as server message format with tiebreaker information.
     */
    fun getStandingsInfo(connectedPlayerIds: Set<EntityId> = emptySet()): List<ServerMessage.PlayerStandingInfo> {
        return getRankedStandings().map { ranked ->
            val s = ranked.standing
            ServerMessage.PlayerStandingInfo(
                playerId = s.playerId.value,
                playerName = s.playerName,
                wins = s.wins,
                losses = s.losses,
                draws = s.draws,
                points = s.points,
                isConnected = connectedPlayerIds.isEmpty() || s.playerId in connectedPlayerIds,
                gamesWon = s.gamesWon,
                gamesLost = s.gamesLost,
                lifeDifferential = s.lifeDifferential,
                rank = ranked.rank,
                tiebreakerReason = if (ranked.tiebreakerReason == TiebreakerReason.NONE) null else ranked.tiebreakerReason.name
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
                player1Id = match.player1Id.value,
                player2Id = match.player2Id?.value,
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

    /**
     * Get the match for a specific player in the current round.
     */
    fun getPlayerMatchInCurrentRound(playerId: EntityId): TournamentMatch? {
        val round = currentRound ?: return null
        return round.matches.find {
            it.player1Id == playerId || it.player2Id == playerId
        }
    }

    /**
     * Peek at the next round's matchups without advancing.
     * Returns a map of playerId -> opponentId (null for BYE).
     */
    fun peekNextRoundMatchups(): Map<EntityId, EntityId?> {
        val nextRoundIndex = currentRoundIndex + 1
        if (nextRoundIndex >= rounds.size) {
            return emptyMap()
        }

        val nextRound = rounds[nextRoundIndex]
        val matchups = mutableMapOf<EntityId, EntityId?>()

        for (match in nextRound.matches) {
            matchups[match.player1Id] = match.player2Id
            if (match.player2Id != null) {
                matchups[match.player2Id] = match.player1Id
            }
        }

        return matchups
    }

    // =========================================================================
    // Persistence Support (for Redis caching)
    // =========================================================================

    /**
     * Get standings for persistence.
     */
    internal fun getStandingsForPersistence(): Map<EntityId, PlayerStanding> = standings.toMap()

    /**
     * Get rounds for persistence.
     */
    internal fun getRoundsForPersistence(): List<TournamentRound> = rounds.toList()

    /**
     * Get current round index for persistence.
     */
    internal fun getCurrentRoundIndexForPersistence(): Int = currentRoundIndex

    /**
     * Get games per match for persistence.
     */
    internal fun getGamesPerMatchForPersistence(): Int = gamesPerMatch

    /**
     * Restore tournament state from persistence.
     * Called when loading a tournament from Redis after server restart.
     */
    internal fun restoreFromPersistence(
        rounds: List<TournamentRound>,
        standings: Map<EntityId, PlayerStanding>,
        currentRoundIndex: Int
    ) {
        this.rounds.clear()
        this.rounds.addAll(rounds)
        this.standings.clear()
        this.standings.putAll(standings)
        this.currentRoundIndex = currentRoundIndex
    }
}
