package com.wingedsheep.ai.arena

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.random.Random

/**
 * Unit tests for the arena's estimators.
 *
 * These matter more than they look: every strength claim this plan makes is read off these three
 * functions, and a subtly wrong interval is worse than no interval — it makes noise look like
 * evidence.
 */
class ArenaStatsTest : FunSpec({

    context("Wilson interval") {
        test("a 50/50 split at n=100 is roughly +/-10 points") {
            val ci = wilsonInterval(50.0, 100)
            abs(ci.low - 0.404) shouldBeLessThan 0.01
            abs(ci.high - 0.596) shouldBeLessThan 0.01
        }

        test("narrows as n grows") {
            val small = wilsonInterval(50.0, 100)
            val large = wilsonInterval(500.0, 1000)
            (large.high - large.low) shouldBeLessThan (small.high - small.low)
        }

        test("stays inside [0, 1] at the extremes, where the normal approximation would not") {
            val perfect = wilsonInterval(20.0, 20)
            perfect.high shouldBe 1.0
            perfect.low shouldBeGreaterThan 0.0
            val shutout = wilsonInterval(0.0, 20)
            shutout.low shouldBe 0.0
            shutout.high shouldBeLessThan 1.0
        }

        test("accepts fractional successes, so draws can count as a half") {
            val ci = wilsonInterval(49.5, 100)
            ci.contains(0.495) shouldBe true
        }

        test("n = 0 is total ignorance, not a crash") {
            wilsonInterval(0.0, 0) shouldBe Interval(0.0, 1.0)
        }
    }

    context("paired bootstrap") {
        test("brackets the sample mean") {
            val samples = List(200) { if (it % 3 == 0) 1.0 else -0.5 }
            val ci = bootstrapMeanCi(samples, 2000, Random(1))
            ci.contains(samples.average()) shouldBe true
        }

        test("is deterministic at a fixed seed — a CI that moves between reruns is not a CI") {
            val samples = List(120) { Random(it).nextDouble(-1.0, 1.0) }
            bootstrapMeanCi(samples, 2000, Random(7)) shouldBe bootstrapMeanCi(samples, 2000, Random(7))
        }

        test("collapses to a point when every pair scored the same") {
            val ci = bootstrapMeanCi(List(50) { 0.0 }, 500, Random(3))
            ci shouldBe Interval(0.0, 0.0)
        }

        test("excludes parity when one side swept") {
            val ci = bootstrapMeanCi(List(200) { 1.0 }, 2000, Random(5))
            ci.low shouldBeGreaterThan 0.0
        }
    }

    context("pair scoring") {
        test("a sweep is +1 and a split is 0, regardless of which seat won") {
            // gameA: agent A in seat 0. gameB: agent B in seat 0, so seat 1 is agent A.
            sweep().score shouldBe 1.0
            ArenaPair(1, game(winnerSeat = 0), game(winnerSeat = 0)).score shouldBe 0.0
            ArenaPair(1, game(winnerSeat = 1), game(winnerSeat = 1)).score shouldBe 0.0
            ArenaPair(1, game(winnerSeat = 1), game(winnerSeat = 0)).score shouldBe -1.0
        }

        test("a draw costs half a point, not a whole one") {
            val pair = ArenaPair(1, game(winnerSeat = 0), game(winnerSeat = null))
            pair.aWins shouldBe 1
            pair.bWins shouldBe 0
            pair.draws shouldBe 1
            pair.score shouldBe 0.5
        }
    }

    context("Bradley-Terry") {
        test("recovers the ordering of a transitive field") {
            val agents = listOf("strong", "middle", "weak")
            val elo = BradleyTerry.elo(agents, listOf(
                Matchup("strong", "middle", 70.0, 100), Matchup("middle", "strong", 30.0, 100),
                Matchup("strong", "weak", 90.0, 100), Matchup("weak", "strong", 10.0, 100),
                Matchup("middle", "weak", 70.0, 100), Matchup("weak", "middle", 30.0, 100),
            ))
            elo.getValue("strong") shouldBeGreaterThan elo.getValue("middle")
            elo.getValue("middle") shouldBeGreaterThan elo.getValue("weak")
        }

        test("rates a perfectly balanced field equally") {
            val agents = listOf("a", "b", "c")
            val elo = BradleyTerry.elo(agents, agents.flatMap { i ->
                agents.filter { it != i }.map { j -> Matchup(i, j, 50.0, 100) }
            })
            elo.values.forEach { abs(it - 1500.0) shouldBeLessThan 1.0 }
        }

        test("gives an agent that never won a finite rating rather than -infinity") {
            val elo = BradleyTerry.elo(listOf("winner", "loser"), listOf(
                Matchup("winner", "loser", 100.0, 100), Matchup("loser", "winner", 0.0, 100),
            ))
            elo.getValue("loser").isFinite() shouldBe true
            elo.getValue("winner") shouldBeGreaterThan elo.getValue("loser")
        }

        test("cannot express a rock-paper-scissors cycle — which is why the matrix is the deliverable") {
            val agents = listOf("rock", "paper", "scissors")
            val elo = BradleyTerry.elo(agents, listOf(
                Matchup("rock", "scissors", 70.0, 100), Matchup("scissors", "rock", 30.0, 100),
                Matchup("scissors", "paper", 70.0, 100), Matchup("paper", "scissors", 30.0, 100),
                Matchup("paper", "rock", 70.0, 100), Matchup("rock", "paper", 30.0, 100),
            ))
            // All three are equal, so Elo alone says "identical strength" about a field where
            // every matchup is 70/30. Report it next to the matrix, never in place of it.
            elo.values.forEach { abs(it - 1500.0) shouldBeLessThan 1.0 }
        }
    }

    context("run seeds") {
        test("adjacent run seeds do not produce adjacent pair seeds") {
            val a = (1..50).map { mixSeed(1L, it.toLong()) }
            val b = (1..50).map { mixSeed(2L, it.toLong()) }
            a.intersect(b.toSet()) shouldBe emptySet()
        }
    }
})

private fun game(winnerSeat: Int?) = ArenaGameOutcome(
    pairId = 1, gameIndex = 0, seat0Agent = "a", seat1Agent = "b", seed = 0L,
    winnerSeat = winnerSeat, turns = 10, actions = 100, durationMs = 1, seat0Life = 0, seat1Life = 0,
    completed = winnerSeat != null, drawReason = "", exception = null, illegalActions = emptyMap(),
    actionStreamHash = null,
)

/** Agent A wins seat 0 in game A and seat 1 in game B. */
private fun sweep() = ArenaPair(1, game(winnerSeat = 0), game(winnerSeat = 1))
