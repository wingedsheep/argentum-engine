package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Reality Hemorrhage — {1}{R} Instant.
 * "Devoid. Reality Hemorrhage deals 2 damage to any target."
 */
class RealityHemorrhageScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Reality Hemorrhage")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("deals 2 damage to a target player") {
            val game = board()

            val result = game.castSpellTargetingPlayer(1, "Reality Hemorrhage", 2)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.isInGraveyard(1, "Reality Hemorrhage") shouldBe true
        }

        test("deals 2 damage to a target creature, killing a 2/2") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Reality Hemorrhage", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
