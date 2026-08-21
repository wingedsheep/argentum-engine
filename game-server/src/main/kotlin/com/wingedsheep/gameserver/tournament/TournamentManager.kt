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
 * Reason why a player's position was determined by a tiebreaker. The order mirrors the official
 * Magic Tournament Rules standings tiebreakers (match points, then OMW%, then GW%, then OGW%).
 * Head-to-head is deliberately *not* used: it is non-transitive (A>B>C>A) and so cannot define a
 * consistent total order.
 */
enum class TiebreakerReason {
    NONE,           // No tie - separated on match points
    OMW,            // Won on opponents' match-win percentage
    GW,             // Won on game-win percentage
    OGW,            // Won on opponents' game-win percentage
    TIED            // True tie - shared position
}

/**
 * Pre-computed standings tiebreaker metrics for one player, per the Magic Tournament Rules. Computing
 * these once up front (rather than via pairwise lookups inside a comparator) keeps the final sort a
 * pure, transitive comparison over plain numbers — the head-to-head comparator this replaces violated
 * the total-ordering contract and could sort arbitrarily (or throw) on a head-to-head cycle.
 *
 *   omw - mean of each opponent's match-win % (each floored at [TournamentManager.MINIMUM_PERCENTAGE]).
 *   gw  - this player's own game-win % (not floored; the floor only protects opponents' averages).
 *   ogw - mean of each opponent's game-win % (each floored at [TournamentManager.MINIMUM_PERCENTAGE]).
 * Byes are excluded from the opponent list, so they never feed OMW%/OGW%.
 */
data class StandingMetrics(
    val points: Int,
    val omw: Double,
    val gw: Double,
    val ogw: Double
)

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
    companion object {
        /**
         * Floor applied to each opponent's percentage when averaging OMW%/OGW% (Magic Tournament Rules:
         * a contributing match-win/game-win percentage is never treated as below 1/3). Keeps a player
         * from being dragged down by an opponent who later collapsed.
         */
        const val MINIMUM_PERCENTAGE: Double = 1.0 / 3.0
    }

    private val standings = players.associate { (id, name) ->
        id to PlayerStanding(id, name)
    }.toMutableMap()

    private val rounds: MutableList<TournamentRound> = mutableListOf()
    private var currentRoundIndex: Int = -1

    var totalRounds: Int
        private set
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

    val isComplete: Boolean get() = rounds.isNotEmpty() && rounds.all { it.isComplete }

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
        // Search all rounds for the match (supports dynamic matchmaking where matches
        // from different rounds may be active simultaneously)
        val match = rounds.asSequence()
            .flatMap { r -> r.matches.asSequence() }
            .find { m -> m.gameSessionId == gameSessionId }
            ?: return

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

    /**
     * Add an extra rotation of round-robin rounds to extend the tournament.
     * Generates the same schedule pattern as the initial rounds.
     */
    fun addExtraRound() {
        val ids = playerIds.toMutableList()
        val hasBye = ids.size % 2 != 0

        val paddedIds: MutableList<EntityId?> = ids.map<EntityId, EntityId?> { it }.toMutableList()
        if (hasBye) {
            paddedIds.add(null)
        }

        val n = paddedIds.size
        val numBaseRounds = n - 1

        var roundNumber = rounds.size
        for (repetition in 0 until gamesPerMatch) {
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
                        matches.add(TournamentMatch(
                            player1Id = p2,
                            player2Id = null
                        ))
                    }
                }

                rounds.add(TournamentRound(roundNumber = roundNumber, matches = matches))

                if (n > 2) {
                    val last = rotatedIds.removeAt(n - 1)
                    rotatedIds.add(1, last)
                }
            }
        }

        totalRounds = rounds.size
        logger.info("Added extra rounds, now $totalRounds total rounds for ${playerIds.size} players in lobby $lobbyId")
    }

    // =========================================================================
    // Tiebreaker Functions
    // =========================================================================

    /**
     * Each player's opponents across all completed, non-bye matches, with repeats preserved (a player
     * met twice in a `gamesPerMatch > 1` schedule counts twice, so OMW%/OGW% average per match as the
     * Magic Tournament Rules intend). Byes are excluded entirely.
     */
    private fun opponentsByPlayer(): Map<EntityId, List<EntityId>> {
        val opponents = playerIds.associateWith { mutableListOf<EntityId>() }
        for (match in rounds.flatMap { it.matches }) {
            if (!match.isComplete || match.isBye) continue
            val p2 = match.player2Id ?: continue
            opponents[match.player1Id]?.add(p2)
            opponents[p2]?.add(match.player1Id)
        }
        return opponents
    }

    /** A player's own match-win % = match points / (3 × matches played); 0 with no matches played. */
    private fun PlayerStanding.matchWinPercentage(): Double {
        val matchesPlayed = wins + losses + draws
        return if (matchesPlayed == 0) 0.0 else points.toDouble() / (3.0 * matchesPlayed)
    }

    /** A player's own game-win % = games won / games played; 0 with no games played. */
    private fun PlayerStanding.gameWinPercentage(): Double {
        val gamesPlayed = gamesWon + gamesLost
        return if (gamesPlayed == 0) 0.0 else gamesWon.toDouble() / gamesPlayed
    }

    /**
     * Compute the Magic Tournament Rules standings tiebreakers for every player. The opponents' averages
     * floor each contributing percentage at [MINIMUM_PERCENTAGE] so a player isn't punished for having
     * faced someone who later collapsed; a player with no completed non-bye matches gets `0.0` for the
     * opponents' metrics.
     */
    private fun computeMetrics(): Map<EntityId, StandingMetrics> {
        val opponents = opponentsByPlayer()
        return standings.mapValues { (id, standing) ->
            val opps = opponents[id].orEmpty()
            val omw = opps.map { maxOf(standings.getValue(it).matchWinPercentage(), MINIMUM_PERCENTAGE) }
                .averageOrZero()
            val ogw = opps.map { maxOf(standings.getValue(it).gameWinPercentage(), MINIMUM_PERCENTAGE) }
                .averageOrZero()
            StandingMetrics(standing.points, omw = omw, gw = standing.gameWinPercentage(), ogw = ogw)
        }
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    /**
     * Determine which Magic Tournament Rules tiebreaker first separated two adjacent players, given the
     * already-computed [metrics]. Returns the first metric in MTR order (OMW% → GW% → OGW%) on which the
     * higher player exceeds the lower; [TiebreakerReason.NONE] if they differ on match points (no tie),
     * or [TiebreakerReason.TIED] if every metric is equal.
     */
    private fun determineTiebreakerUsed(
        higher: PlayerStanding,
        lower: PlayerStanding,
        metrics: Map<EntityId, StandingMetrics>
    ): TiebreakerReason {
        val h = metrics.getValue(higher.playerId)
        val l = metrics.getValue(lower.playerId)
        return when {
            h.points != l.points -> TiebreakerReason.NONE
            h.omw > l.omw -> TiebreakerReason.OMW
            h.gw > l.gw -> TiebreakerReason.GW
            h.ogw > l.ogw -> TiebreakerReason.OGW
            else -> TiebreakerReason.TIED
        }
    }

    /**
     * Current standings sorted by the Magic Tournament Rules tiebreaker order:
     * 1. Match points (wins × 3 + draws)
     * 2. Opponents' match-win % (OMW%)
     * 3. Game-win % (GW%)
     * 4. Opponents' game-win % (OGW%)
     *
     * The comparison is a pure, transitive order over pre-computed numbers, so — unlike the old
     * head-to-head comparator — it can never violate the sort contract on a head-to-head cycle.
     */
    fun getStandings(): List<PlayerStanding> = getStandings(computeMetrics())

    private fun getStandings(metrics: Map<EntityId, StandingMetrics>): List<PlayerStanding> {
        fun key(s: PlayerStanding) = metrics.getValue(s.playerId)
        return standings.values.sortedWith(
            compareByDescending<PlayerStanding> { key(it).points }
                .thenByDescending { key(it).omw }
                .thenByDescending { key(it).gw }
                .thenByDescending { key(it).ogw }
        )
    }

    /**
     * Get ranked standings with tiebreaker information.
     * Players who are truly tied share the same rank.
     */
    fun getRankedStandings(): List<RankedStanding> {
        val metrics = computeMetrics()
        val sorted = getStandings(metrics)
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
                tiebreakerReason = determineTiebreakerUsed(previous, standing, metrics)

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
        val metrics = computeMetrics()
        return getRankedStandings().map { ranked ->
            val s = ranked.standing
            val m = metrics.getValue(s.playerId)
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
                omwPercent = m.omw,
                gwPercent = m.gw,
                ogwPercent = m.ogw,
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
     * Get all completed game session IDs across all rounds.
     */
    fun getCompletedGameSessionIds(): List<String> {
        return rounds.flatMap { it.matches }
            .filter { it.isComplete && !it.isBye && it.gameSessionId != null }
            .mapNotNull { it.gameSessionId }
    }

    /**
     * Get the non-BYE matches for the current round that need game sessions.
     */
    fun getCurrentRoundGameMatches(): List<TournamentMatch> {
        val round = currentRound ?: return emptyList()
        return round.matches.filter { !it.isBye && !it.isComplete }
    }

    /**
     * Get all in-progress matches across all rounds (started but not complete).
     * With eager match starting, games from future rounds may already be running
     * while the current round isn't finished yet.
     */
    fun getAllInProgressMatches(): List<TournamentMatch> {
        return rounds.flatMap { it.matches }
            .filter { !it.isBye && !it.isComplete && it.gameSessionId != null }
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
     * Check if a player has any incomplete (not yet finished) match in rounds
     * before the given round number. Used to prevent cross-round matchmaking
     * from stranding opponents who still have earlier-round games to play.
     */
    fun hasIncompleteMatchBefore(playerId: EntityId, beforeRoundNumber: Int): Boolean {
        for (round in rounds) {
            if (round.roundNumber >= beforeRoundNumber) break
            val match = round.matches.find {
                (it.player1Id == playerId || it.player2Id == playerId) && !it.isComplete
            }
            if (match != null) return true
        }
        return false
    }

    /**
     * Check if a player has any in-progress match (started but not complete) across all rounds.
     */
    fun hasActiveMatch(playerId: EntityId): Boolean {
        return rounds.any { round ->
            round.matches.any {
                (it.player1Id == playerId || it.player2Id == playerId) &&
                    it.gameSessionId != null && !it.isComplete
            }
        }
    }

    /**
     * Get the next unplayed match for a player across all rounds.
     * Returns the round and match, or null if no remaining matches.
     */
    fun getNextMatchForPlayer(playerId: EntityId): Pair<TournamentRound, TournamentMatch>? {
        for (round in rounds) {
            val match = round.matches.find {
                (it.player1Id == playerId || it.player2Id == playerId) &&
                    !it.isComplete && it.gameSessionId == null
            }
            if (match != null) return round to match
        }
        return null
    }

    /**
     * Every match that could be launched right now, given the set of players who are ready.
     *
     * A match qualifies when it has two seats (no BYE), hasn't been started or decided, both seats
     * are ready, and neither seat still owes a game from an earlier round — see
     * [hasIncompleteMatchBefore]. That last guard is why this query exists rather than a per-player
     * lookup: a pair can become startable purely because somebody *else's* earlier-round match
     * finished, with no change to the ready set, so the whole set has to be re-examined whenever a
     * result lands.
     *
     * A player appears in at most one returned pair: their earliest unplayed match blocks every
     * later one through the same guard, so the caller can start all of them in one sweep.
     */
    fun startableMatches(readyPlayerIds: Set<EntityId>): List<Pair<TournamentRound, TournamentMatch>> {
        val startable = mutableListOf<Pair<TournamentRound, TournamentMatch>>()
        for (round in rounds) {
            for (match in round.matches) {
                if (match.isBye || match.isComplete || match.gameSessionId != null) continue
                val player2Id = match.player2Id ?: continue
                if (match.player1Id !in readyPlayerIds || player2Id !in readyPlayerIds) continue
                if (hasIncompleteMatchBefore(match.player1Id, round.roundNumber)) continue
                if (hasIncompleteMatchBefore(player2Id, round.roundNumber)) continue
                startable.add(round to match)
            }
        }
        return startable
    }

    /**
     * Get the round containing a specific match (by gameSessionId).
     */
    fun getRoundForMatch(gameSessionId: String): TournamentRound? {
        return rounds.find { round ->
            round.matches.any { it.gameSessionId == gameSessionId }
        }
    }

    /**
     * Get match results for a specific round.
     */
    fun getRoundResults(round: TournamentRound): List<ServerMessage.MatchResultInfo> {
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
