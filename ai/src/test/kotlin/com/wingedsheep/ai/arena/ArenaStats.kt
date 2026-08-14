package com.wingedsheep.ai.arena

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/** A two-sided confidence interval. */
data class Interval(val low: Double, val high: Double) {
    fun contains(value: Double) = value in low..high
    override fun toString() = "[${fmt("%.3f", low)}, ${fmt("%.3f", high)}]"
}

/**
 * One pair: the same decks and the same game seed played twice with the seats swapped.
 *
 * [gameA] seats agent A in seat 0; [gameB] seats agent B in seat 0. Pairing is the estimator — the
 * first-player advantage and the deck draw are identical across the two games, so they cancel.
 */
data class ArenaPair(val pairId: Int, val gameA: ArenaGameOutcome, val gameB: ArenaGameOutcome) {
    val games: List<ArenaGameOutcome> get() = listOf(gameA, gameB)

    /** Games agent A won across the pair (0-2). */
    val aWins: Int = (if (gameA.winnerSeat == 0) 1 else 0) + (if (gameB.winnerSeat == 1) 1 else 0)

    /** Games agent B won across the pair (0-2). */
    val bWins: Int = (if (gameA.winnerSeat == 1) 1 else 0) + (if (gameB.winnerSeat == 0) 1 else 0)

    val draws: Int = 2 - aWins - bWins

    /**
     * Agent A's score for the pair, in [-1, 1]: +1 swept, 0 split, -1 swept against. Draws land on
     * the halves. This — not the per-game win rate — is the quantity the merge gate is read off.
     */
    val score: Double = (aWins - bWins) / 2.0
}

/**
 * Everything a head-to-head arena run reports. Two independent estimators of the same thing (paired
 * and unpaired) plus the health signals that say whether either can be trusted.
 */
data class ArenaStats(
    val agentA: String,
    val agentB: String,
    val pairs: Int,
    val games: Int,
    val aWins: Int,
    val bWins: Int,
    val draws: Int,
    /** Agent A's per-game score rate, draws counted as a half. Unpaired. */
    val gameScoreRate: Double,
    /** Wilson score interval on [gameScoreRate]. */
    val gameScoreCi: Interval,
    /** Mean of [ArenaPair.score]. 0.0 is parity. */
    val meanPairScore: Double,
    /** Paired bootstrap percentile interval on [meanPairScore]. */
    val pairScoreCi: Interval,
    /** [meanPairScore] rescaled to a 0-1 win-rate-like number, so 0.5 is parity. */
    val pairWinShare: Double,
    val pairWinShareCi: Interval,
    /** Games won by whoever sat in seat 0. A seat/seed leak shows up here first. */
    val seat0Wins: Int,
    val completedGames: Int,
    /** Rejected AI actions by `"<ActionType>: <error>"`. Every entry is a bug worth a look. */
    val illegalActions: Map<String, Int>,
    val drawReasons: Map<String, Int>,
    val exceptions: Map<String, Int>,
    val meanTurns: Double,
    val meanActions: Double,
    val meanGameMs: Double,
) {
    val seat0WinRate: Double get() = if (games > 0) seat0Wins.toDouble() / games else 0.0
    val completionRate: Double get() = if (games > 0) completedGames.toDouble() / games else 0.0

    /** The merge gate: agent A is a demonstrated improvement only if the whole interval clears parity. */
    val beatsOpponent: Boolean get() = pairWinShareCi.low > 0.5

    companion object {
        /** Resamples for the paired bootstrap. Fixed so a rerun of the same games reports the same CI. */
        const val BOOTSTRAP_RESAMPLES = 2_000

        fun of(agentA: String, agentB: String, pairs: List<ArenaPair>, bootstrapSeed: Long = 20260727L): ArenaStats {
            val games = pairs.flatMap { it.games }
            val aWins = pairs.sumOf { it.aWins }
            val bWins = pairs.sumOf { it.bWins }
            val drawCount = pairs.sumOf { it.draws }
            val scoreSuccesses = aWins + drawCount / 2.0
            val pairScores = pairs.map { it.score }

            val pairCi = bootstrapMeanCi(pairScores, BOOTSTRAP_RESAMPLES, Random(bootstrapSeed))
            return ArenaStats(
                agentA = agentA,
                agentB = agentB,
                pairs = pairs.size,
                games = games.size,
                aWins = aWins,
                bWins = bWins,
                draws = drawCount,
                gameScoreRate = if (games.isEmpty()) 0.0 else scoreSuccesses / games.size,
                gameScoreCi = wilsonInterval(scoreSuccesses, games.size),
                meanPairScore = pairScores.average(),
                pairScoreCi = pairCi,
                pairWinShare = (pairScores.average() + 1.0) / 2.0,
                pairWinShareCi = Interval((pairCi.low + 1.0) / 2.0, (pairCi.high + 1.0) / 2.0),
                seat0Wins = games.count { it.winnerSeat == 0 },
                completedGames = games.count { it.completed },
                illegalActions = games.flatMap { it.illegalActions.entries }
                    .groupingBy { it.key }.fold(0) { total, e -> total + e.value }
                    .toList().sortedByDescending { it.second }.toMap(),
                drawReasons = games.filter { it.drawReason.isNotEmpty() }
                    .groupingBy { it.drawReason.substringBefore('(') }.eachCount()
                    .toList().sortedByDescending { it.second }.toMap(),
                exceptions = games.mapNotNull { it.exception }
                    .groupingBy { it }.eachCount()
                    .toList().sortedByDescending { it.second }.toMap(),
                meanTurns = games.map { it.turns }.averageOrZero(),
                meanActions = games.map { it.actions }.averageOrZero(),
                meanGameMs = games.map { it.durationMs }.averageOrZero(),
            )
        }
    }
}

private fun List<Int>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

@JvmName("averageOrZeroLong")
private fun List<Long>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

// ─────────────────────────────────────────────────────────────────────────────
// Estimators
// ─────────────────────────────────────────────────────────────────────────────

/** 95% two-sided normal quantile. */
private const val Z_95 = 1.959963984540054

/**
 * Wilson score interval — the right one for a proportion at small n or near 0/1, where the normal
 * approximation runs off the end of the scale.
 *
 * [successes] is a Double so draws can count as a half; the formula is unchanged by that.
 */
internal fun wilsonInterval(successes: Double, n: Int, z: Double = Z_95): Interval {
    if (n <= 0) return Interval(0.0, 1.0)
    val p = successes / n
    val z2 = z * z
    val denominator = 1.0 + z2 / n
    val center = (p + z2 / (2 * n)) / denominator
    val margin = z / denominator * sqrt(p * (1 - p) / n + z2 / (4.0 * n * n))
    return Interval((center - margin).coerceAtLeast(0.0), (center + margin).coerceAtMost(1.0))
}

/**
 * Percentile bootstrap interval for the mean of [samples].
 *
 * Resampling **whole pairs** is what makes this the paired estimator: the two games in a pair move
 * together, so the between-game correlation that pairing bought is preserved in the resample.
 */
internal fun bootstrapMeanCi(
    samples: List<Double>,
    resamples: Int,
    rng: Random,
    alpha: Double = 0.05,
): Interval {
    if (samples.isEmpty()) return Interval(-1.0, 1.0)
    if (samples.size == 1) return Interval(samples[0], samples[0])
    val means = DoubleArray(resamples) {
        var sum = 0.0
        repeat(samples.size) { sum += samples[rng.nextInt(samples.size)] }
        sum / samples.size
    }
    means.sort()
    val lowIndex = ((alpha / 2) * resamples).toInt().coerceIn(0, resamples - 1)
    val highIndex = ((1 - alpha / 2) * resamples).toInt().coerceIn(0, resamples - 1)
    return Interval(means[lowIndex], means[highIndex])
}

// ─────────────────────────────────────────────────────────────────────────────
// Gauntlet rating
// ─────────────────────────────────────────────────────────────────────────────

/** One cell of the gauntlet matrix: [row]'s record against [column]. */
data class Matchup(val row: String, val column: String, val wins: Double, val games: Int) {
    val winRate: Double get() = if (games > 0) wins / games else 0.0
}

/**
 * Bradley-Terry strengths fitted by iterative MM (minorization-maximization).
 *
 * ~40 lines and no dependency. Report it **alongside the full pairwise matrix, never instead of
 * it**: MTG agents are frequently non-transitive — an aggressive agent beats a controlling one that
 * beats a midrange one that beats the aggressive one — and a single rating number erases exactly
 * that structure.
 */
object BradleyTerry {

    private const val MAX_ITERATIONS = 500
    private const val CONVERGENCE = 1e-9

    /** Floor on a strength, so an agent that never won maps to a finite (very low) Elo. */
    private const val MIN_STRENGTH = 1e-6

    /**
     * @param matchups **one entry per ordered pair** — both `(X vs Y)` and `(Y vs X)` — with draws
     *   already split as half a win to each side. [ArenaReport] builds them that way.
     * @return Elo-scaled ratings, centred on 1500.
     */
    fun elo(agents: List<String>, matchups: List<Matchup>): Map<String, Double> {
        val strengths = agents.associateWith { 1.0 }.toMutableMap()
        val wins = agents.associateWith { 0.0 }.toMutableMap()
        val played = mutableMapOf<Pair<String, String>, Int>()
        for (m in matchups) {
            wins[m.row] = wins.getValue(m.row) + m.wins
            played[m.row to m.column] = (played[m.row to m.column] ?: 0) + m.games
        }

        for (iteration in 0 until MAX_ITERATIONS) {
            val next = agents.associateWith { i ->
                var denominator = 0.0
                for (j in agents) {
                    if (i == j) continue
                    val n = played[i to j] ?: continue
                    denominator += n / (strengths.getValue(i) + strengths.getValue(j))
                }
                if (denominator > 0.0) wins.getValue(i) / denominator else strengths.getValue(i)
            }
            // Normalize to geometric mean 1 — BT strengths are only identified up to a scale
            // factor, and without this they drift toward 0 or infinity over the iterations.
            val floored = next.mapValues { it.value.coerceAtLeast(MIN_STRENGTH) }
            val scale = 10.0.pow(-floored.values.sumOf { log10(it) } / floored.size)
            var maxDelta = 0.0
            for (i in agents) {
                val value = (floored.getValue(i) * scale).coerceAtLeast(MIN_STRENGTH)
                maxDelta = maxOf(maxDelta, abs(value - strengths.getValue(i)))
                strengths[i] = value
            }
            if (maxDelta < CONVERGENCE) break
        }

        return agents.associateWith { 1500.0 + 400.0 * log10(strengths.getValue(it)) }
    }
}
