package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Skybeast Tracker. */
class SkybeastTrackerScenarioTest : ScenarioTestBase() {

    init {
        context("Skybeast Tracker — Food only on mana value 5 or greater") {
            test("casting a 6-drop creates a Food token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skybeast Tracker", summoningSickness = false)
                    .withCardInHand(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.findPermanents("Food").size shouldBe 0

                // Craw Wurm is {4}{G}{G} — mana value 6.
                game.castSpell(1, "Craw Wurm")
                game.resolveStack()

                withClue("the cast trigger made exactly one Food") {
                    game.findPermanents("Food").size shouldBe 1
                }
            }

            test("casting a 4-drop makes no Food — the threshold is 5, not 4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skybeast Tracker", summoningSickness = false)
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Hill Giant is {3}{R} — mana value 4.
                game.castSpell(1, "Hill Giant")
                game.resolveStack()

                game.findPermanents("Food").size shouldBe 0
            }
        }
    }
}
