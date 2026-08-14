package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Protective Parents. */
class ProtectiveParentsScenarioTest : ScenarioTestBase() {

    init {
        context("Protective Parents — a Young Hero Role on death") {
            test("dying creates the Role on the chosen creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Protective Parents", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val parents = game.findPermanent("Protective Parents")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // 3 damage kills the 3/2 Parents.
                game.castSpell(1, "Lightning Bolt", parents).error shouldBe null
                game.resolveStack()

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the Parents died and their trigger made a Young Hero Role") {
                    game.isInGraveyard(1, "Protective Parents") shouldBe true
                    (game.findPermanent("Young Hero Role") != null) shouldBe true
                }
            }

            test("'up to one' may be declined — no Role is created") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Protective Parents", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val parents = game.findPermanent("Protective Parents")!!

                game.castSpell(1, "Lightning Bolt", parents).error shouldBe null
                game.resolveStack()

                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("declining the optional target creates nothing") {
                    (game.findPermanent("Young Hero Role") == null) shouldBe true
                }
            }
        }
    }
}
