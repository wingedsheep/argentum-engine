package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Overwhelming Forces (PTK) — destroys only the target opponent's creatures
 * and draws a card per creature actually destroyed.
 */
class OverwhelmingForcesScenarioTest : ScenarioTestBase() {

    init {
        context("Overwhelming Forces") {

            test("destroys opponent's creatures only and draws a card for each") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Overwhelming Forces")
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Overwhelming Forces", targetPlayerNumber = 2).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 2
            }

            test("no opposing creatures draws nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Overwhelming Forces")
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Overwhelming Forces", targetPlayerNumber = 2).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 0
            }
        }
    }
}
