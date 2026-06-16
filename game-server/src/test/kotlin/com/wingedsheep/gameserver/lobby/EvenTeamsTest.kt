package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * Even-team partition (CR 810 for Two-Headed Giant, CR 808 for Team vs. Team). [EvenTeams.partition]
 * is the single source of truth FreeForAllHandler uses to fill `GameSession.teams`: random by
 * default, or the host's manual playerId -> team assignment with a safe fallback for malformed setups.
 */
class EvenTeamsTest : FunSpec({

    val ids = (0..3).map { EntityId.of("p$it") }

    fun flatten(teams: List<List<Int>>) = teams.flatten().sorted()

    test("random teams cover all four seats as two disjoint pairs (2HG / 2v2)") {
        // A fixed seed keeps the assertion deterministic without pinning a specific shuffle order.
        val teams = EvenTeams.partition(ids, randomTeams = true, manualAssignment = emptyMap(), random = Random(42))
        teams.size shouldBe 2
        teams.forEach { it.size shouldBe 2 }
        flatten(teams) shouldBe listOf(0, 1, 2, 3)
    }

    test("manual assignment places each player on the chosen team") {
        val assignment = mapOf(ids[0] to 1, ids[1] to 1, ids[2] to 0, ids[3] to 0)
        val teams = EvenTeams.partition(ids, randomTeams = false, manualAssignment = assignment)
        teams[0] shouldContainExactlyInAnyOrder listOf(2, 3)
        teams[1] shouldContainExactlyInAnyOrder listOf(0, 1)
    }

    test("a partial manual assignment balances the rest into the open team") {
        val teams = EvenTeams.partition(ids, randomTeams = false, manualAssignment = mapOf(ids[0] to 0))
        teams.forEach { it.size shouldBe 2 }
        teams[0] shouldBe listOf(0, 1)
        teams[1] shouldBe listOf(2, 3)
    }

    test("a malformed assignment (three on one team) falls back to seat-order pairing") {
        val assignment = mapOf(ids[0] to 0, ids[1] to 0, ids[2] to 0)
        val teams = EvenTeams.partition(ids, randomTeams = false, manualAssignment = assignment)
        teams shouldBe listOf(listOf(0, 1), listOf(2, 3))
    }

    test("an out-of-range team index is ignored, then balanced") {
        val teams = EvenTeams.partition(ids, randomTeams = false, manualAssignment = mapOf(ids[0] to 7))
        teams.forEach { it.size shouldBe 2 }
        flatten(teams) shouldBe listOf(0, 1, 2, 3)
    }

    // Team vs. Team supports larger even pods (CR 808.1): the same partition splits 6 seats into 3v3
    // and 8 seats into 4v4.
    test("a six-player pod splits into two teams of three (3v3)") {
        val six = (0..5).map { EntityId.of("p$it") }
        val teams = EvenTeams.partition(six, randomTeams = true, manualAssignment = emptyMap(), random = Random(7))
        teams.size shouldBe 2
        teams.forEach { it.size shouldBe 3 }
        flatten(teams) shouldBe (0..5).toList()
    }

    test("an eight-player manual assignment yields two teams of four (4v4)") {
        val eight = (0..7).map { EntityId.of("p$it") }
        val assignment = mapOf(
            eight[0] to 0, eight[2] to 0, eight[4] to 0, eight[6] to 0,
            eight[1] to 1, eight[3] to 1, eight[5] to 1, eight[7] to 1,
        )
        val teams = EvenTeams.partition(eight, randomTeams = false, manualAssignment = assignment)
        teams[0] shouldContainExactlyInAnyOrder listOf(0, 2, 4, 6)
        teams[1] shouldContainExactlyInAnyOrder listOf(1, 3, 5, 7)
    }
})
