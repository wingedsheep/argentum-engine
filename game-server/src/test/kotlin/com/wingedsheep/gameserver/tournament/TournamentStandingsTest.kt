package com.wingedsheep.gameserver.tournament

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Standings tiebreaker behaviour, per the Magic Tournament Rules order
 * (match points → OMW% → GW% → OGW%). The headline case is the head-to-head *cycle*, which the
 * previous head-to-head comparator could not order consistently (it violated the sort contract).
 */
class TournamentStandingsTest : FunSpec({

    // Drives the full round-robin to completion, deciding each non-bye match with [winnerOf]
    // (null = draw). Byes auto-complete in startNextRound and award no points.
    fun TournamentManager.playOut(winnerOf: (EntityId, EntityId) -> EntityId?) {
        var game = 0
        while (true) {
            val round = startNextRound() ?: break
            for (match in round.matches) {
                if (match.isBye) continue
                val p2 = match.player2Id ?: continue
                val sessionId = "g${game++}"
                match.gameSessionId = sessionId
                reportMatchResult(sessionId, winnerOf(match.player1Id, p2))
            }
        }
    }

    fun pair(p1: EntityId, p2: EntityId, a: EntityId, b: EntityId) =
        (p1 == a && p2 == b) || (p1 == b && p2 == a)

    test("a head-to-head cycle sorts deterministically and never throws") {
        // A beat B, B beat C, C beat A: each 1-1 (3 pts), identical on every metric. The old
        // head-to-head comparator could throw "Comparison method violates its general contract" here.
        val a = EntityId("A"); val b = EntityId("B"); val c = EntityId("C")
        val manager = TournamentManager("lobby", listOf(a to "Alice", b to "Bob", c to "Cara"))

        manager.playOut { p1, p2 ->
            when {
                pair(p1, p2, a, b) -> a
                pair(p1, p2, b, c) -> b
                pair(p1, p2, c, a) -> c
                else -> null
            }
        }

        val standings = manager.getStandings() // must not throw
        standings.map { it.points }.toSet() shouldBe setOf(3)

        val ranked = manager.getRankedStandings()
        ranked shouldNotBe emptyList<RankedStanding>()
        // Truly tied → everyone shares rank 1; followers are reported as TIED.
        ranked.all { it.rank == 1 } shouldBe true
        ranked.drop(1).all { it.tiebreakerReason == TiebreakerReason.TIED } shouldBe true
    }

    test("game-win % breaks a tie that match points and OMW% cannot") {
        // Best of three. A and B draw their match (a game each and a drawn game), then both beat C and
        // D — A 2-0 each time, B 2-1. Same 7 points, and since A and B have the same match-win %, the
        // same OMW% over a shared opponent pool. A dropped fewer games, so A ranks ahead with reason GW.
        // (In best-of-one, GW% mirrors match-win %, so it can only break ties in longer matches.)
        val a = EntityId("A"); val b = EntityId("B")
        val c = EntityId("C"); val d = EntityId("D")
        val manager = TournamentManager("lobby", listOf(a to "A", b to "B", c to "C", d to "D"), bestOf = 3)

        // Each match's games in order, as the winner of each game (null = drawn game).
        fun games(p1: EntityId, p2: EntityId): List<EntityId?> = when {
            pair(p1, p2, a, b) -> listOf(a, b, null)
            pair(p1, p2, a, c) || pair(p1, p2, a, d) -> listOf(a, a)
            pair(p1, p2, b, c) -> listOf(b, c, b)
            pair(p1, p2, b, d) -> listOf(b, d, b)
            else -> listOf(c, c) // C beats D
        }
        var game = 0
        while (true) {
            val round = manager.startNextRound() ?: break
            for (match in round.matches) {
                val p2 = match.player2Id ?: continue
                for (winner in games(match.player1Id, p2)) {
                    val sessionId = "g${game++}"
                    match.gameSessionId = sessionId
                    manager.reportMatchResult(sessionId, winner)
                }
                match.isComplete shouldBe true
            }
        }

        val byName = manager.getStandingsInfo().associateBy { it.playerName }

        byName.getValue("A").points shouldBe 7
        byName.getValue("B").points shouldBe 7
        byName.getValue("A").omwPercent shouldBe (byName.getValue("B").omwPercent plusOrMinus 1e-9)
        // A: 5 won, 1 lost, 1 drawn → 16/21. B: 5 won, 3 lost, 1 drawn → 16/27.
        byName.getValue("A").gwPercent shouldBe (16.0 / 21.0 plusOrMinus 1e-9)
        byName.getValue("B").gwPercent shouldBe (16.0 / 27.0 plusOrMinus 1e-9)
        byName.getValue("A").rank shouldBe (byName.getValue("B").rank - 1)
        byName.getValue("B").tiebreakerReason shouldBe TiebreakerReason.GW.name
    }

    test("OMW% floors each opponent's match-win % at 1/3 and excludes byes") {
        // Three players (each round one byes): A 2-0, B 1-1, C 0-2.
        // OMW(A) averages B's MW% (.5) with C's MW% (0 → floored to 1/3).
        val a = EntityId("A"); val b = EntityId("B"); val c = EntityId("C")
        val manager = TournamentManager("lobby", listOf(a to "A", b to "B", c to "C"))

        manager.playOut { p1, p2 ->
            when {
                pair(p1, p2, a, b) -> a
                pair(p1, p2, a, c) -> a
                pair(p1, p2, b, c) -> b
                else -> null
            }
        }

        val info = manager.getStandingsInfo().associateBy { it.playerName }
        // (0.5 + 1/3) / 2 — the floor lifts C's 0% up to 1/3.
        info.getValue("A").omwPercent shouldBe (((0.5 + 1.0 / 3.0) / 2.0) plusOrMinus 1e-9)
    }
})
