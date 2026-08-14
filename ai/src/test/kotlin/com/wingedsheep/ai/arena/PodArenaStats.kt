package com.wingedsheep.ai.arena

import kotlin.random.Random

/**
 * Everything a multiplayer pod run reports.
 *
 * The number that decides anything is [winShare] against [nullShare] — **not** against 50%. Agent A
 * holds one seat in a field of agent B, so parity at a three-seat table is a 33.3% win share and at
 * a four-seat free-for-all it is 25%.
 */
data class PodArenaStats(
    val agentA: String,
    val agentB: String,
    val table: String,
    val groups: Int,
    val games: Int,
    val aWins: Int,
    /** Games nobody won: draws, max-turns, stuck detections, engine exceptions. */
    val noWinner: Int,
    /** A's share of all games played. Unfinished games drag this below [nullShare] for everyone. */
    val winShare: Double,
    /**
     * A's share of the games that produced a winner. The cleaner read of relative strength — at
     * `maxTurns` a pod frequently times out, and a timeout is not a result for anyone. Quoted
     * alongside [winShare], never instead of it: a change that wins more but finishes less is
     * something you want to see.
     */
    val decisiveWinShare: Double,
    /**
     * Bootstrap percentile interval on [winShare], resampling **whole rotation groups**. Resampling
     * games instead would break the rotation apart and throw away the pairing it buys.
     */
    val winShareCi: Interval,
    /** Parity: 1 / number of teams at the table. */
    val nullShare: Double,
    /** Games won per team position — a seat/turn-order leak shows up here first. */
    val winsByTeamPosition: List<Int>,
    val completedGames: Int,
    val illegalActions: Map<String, Int>,
    val drawReasons: Map<String, Int>,
    val exceptions: Map<String, Int>,
    val meanTurns: Double,
    val meanActions: Double,
    val meanGameMs: Double,
) {
    val completionRate: Double get() = if (games > 0) completedGames.toDouble() / games else 0.0

    /** A is a demonstrated improvement only if the whole interval clears the null share. */
    val beatsField: Boolean get() = winShareCi.low > nullShare

    companion object {
        fun of(config: PodArenaConfig, groups: List<PodGroup>, bootstrapSeed: Long = 20260727L): PodArenaStats {
            val games = groups.flatMap { it.games }
            val outcomes = games.map { it.outcome }
            val shares = groups.map { it.share }
            val ci = bootstrapMeanCi(shares, ArenaStats.BOOTSTRAP_RESAMPLES, Random(bootstrapSeed))
            val aWins = games.count { it.aWon }
            val decisive = outcomes.count { it.winnerTeam != null }

            return PodArenaStats(
                agentA = config.agentA.name,
                agentB = config.agentB.name,
                table = config.table.id,
                groups = groups.size,
                games = games.size,
                aWins = aWins,
                noWinner = games.size - decisive,
                winShare = if (games.isEmpty()) 0.0 else aWins.toDouble() / games.size,
                decisiveWinShare = if (decisive == 0) 0.0 else aWins.toDouble() / decisive,
                winShareCi = ci,
                nullShare = config.nullShare,
                winsByTeamPosition = (0 until config.table.teamCount).map { team ->
                    outcomes.count { it.winnerTeam == team }
                },
                completedGames = outcomes.count { it.completed },
                illegalActions = outcomes.flatMap { it.illegalActions.entries }
                    .groupingBy { it.key }.fold(0) { total, e -> total + e.value }
                    .toList().sortedByDescending { it.second }.toMap(),
                drawReasons = outcomes.filter { it.drawReason.isNotEmpty() }
                    .groupingBy { it.drawReason.substringBefore('(') }.eachCount()
                    .toList().sortedByDescending { it.second }.toMap(),
                exceptions = outcomes.mapNotNull { it.exception }
                    .groupingBy { it }.eachCount()
                    .toList().sortedByDescending { it.second }.toMap(),
                meanTurns = if (outcomes.isEmpty()) 0.0 else outcomes.map { it.turns }.average(),
                meanActions = if (outcomes.isEmpty()) 0.0 else outcomes.map { it.actions }.average(),
                meanGameMs = if (outcomes.isEmpty()) 0.0 else outcomes.map { it.durationMs }.average(),
            )
        }
    }
}
