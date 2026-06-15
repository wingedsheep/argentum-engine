package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * Two-Headed Giant team partition (CR 810). [TwoHeadedGiantTeams.partition] is the single source of
 * truth FreeForAllHandler uses to fill `GameSession.teams`: random by default, or the host's manual
 * playerId -> team assignment with a safe fallback for malformed setups.
 */
class TwoHeadedGiantTeamsTest : FunSpec({

    val ids = (0..3).map { EntityId.of("p$it") }

    fun flatten(teams: List<List<Int>>) = teams.flatten().sorted()

    test("random teams cover all four seats as two disjoint pairs") {
        // A fixed seed keeps the assertion deterministic without pinning a specific shuffle order.
        val teams = TwoHeadedGiantTeams.partition(ids, randomTeams = true, manualAssignment = emptyMap(), random = Random(42))
        teams.size shouldBe 2
        teams.forEach { it.size shouldBe 2 }
        flatten(teams) shouldBe listOf(0, 1, 2, 3)
    }

    test("manual assignment places each player on the chosen team") {
        val assignment = mapOf(ids[0] to 1, ids[1] to 1, ids[2] to 0, ids[3] to 0)
        val teams = TwoHeadedGiantTeams.partition(ids, randomTeams = false, manualAssignment = assignment)
        teams[0] shouldContainExactlyInAnyOrder listOf(2, 3)
        teams[1] shouldContainExactlyInAnyOrder listOf(0, 1)
    }

    test("a partial manual assignment balances the rest into the open team") {
        val teams = TwoHeadedGiantTeams.partition(ids, randomTeams = false, manualAssignment = mapOf(ids[0] to 0))
        teams.forEach { it.size shouldBe 2 }
        teams[0] shouldBe listOf(0, 1)
        teams[1] shouldBe listOf(2, 3)
    }

    test("a malformed assignment (three on one team) falls back to seat-order pairing") {
        val assignment = mapOf(ids[0] to 0, ids[1] to 0, ids[2] to 0)
        val teams = TwoHeadedGiantTeams.partition(ids, randomTeams = false, manualAssignment = assignment)
        teams shouldBe listOf(listOf(0, 1), listOf(2, 3))
    }

    test("an out-of-range team index is ignored, then balanced") {
        val teams = TwoHeadedGiantTeams.partition(ids, randomTeams = false, manualAssignment = mapOf(ids[0] to 7))
        teams.forEach { it.size shouldBe 2 }
        flatten(teams) shouldBe listOf(0, 1, 2, 3)
    }
})
