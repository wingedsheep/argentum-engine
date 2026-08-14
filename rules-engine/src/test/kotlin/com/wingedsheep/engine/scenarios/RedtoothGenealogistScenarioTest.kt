package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Redtooth Genealogist. */
class RedtoothGenealogistScenarioTest : ScenarioTestBase() {

    init {
        context("Redtooth Genealogist — a Royal Role for another creature you control") {
            test("the Role lands on the other creature and grants +1/+1 and ward") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Redtooth Genealogist")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Redtooth Genealogist").error shouldBe null
                game.resolveStack() // Genealogist enters -> ETB trigger asks for its target

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("a Royal Role token was created") {
                    (game.findPermanent("Royal Role") != null) shouldBe true
                }
                withClue("the Bears are 2/2 base plus the Role's +1/+1") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
                withClue("and the Genealogist itself stayed a plain 2/3 — it can't crown itself") {
                    val genealogist = game.findPermanent("Redtooth Genealogist")!!
                    game.state.projectedState.getPower(genealogist) shouldBe 2
                    game.state.projectedState.getToughness(genealogist) shouldBe 3
                }
            }

            test("with no other creature there is no legal target and no Role appears") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Redtooth Genealogist")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Redtooth Genealogist").error shouldBe null
                game.resolveStack()

                withClue("'another target creature you control' has no legal choice") {
                    (game.findPermanent("Royal Role") == null) shouldBe true
                }
            }
        }
    }
}
