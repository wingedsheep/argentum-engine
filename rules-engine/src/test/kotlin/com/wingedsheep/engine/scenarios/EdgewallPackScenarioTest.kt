package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Edgewall Pack. */
class EdgewallPackScenarioTest : ScenarioTestBase() {

    init {
        context("Edgewall Pack — menace plus a can't-block Rat") {
            test("entering the battlefield creates a 1/1 black Rat that can't block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Edgewall Pack")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Edgewall Pack")
                game.resolveStack()

                val pack = game.findPermanent("Edgewall Pack")!!
                withClue("Edgewall Pack has menace") {
                    game.state.projectedState.hasKeyword(pack, Keyword.MENACE) shouldBe true
                }

                val rat = game.findPermanent("Rat Token")
                withClue("the ETB trigger created a Rat token") { (rat != null) shouldBe true }

                withClue("the Rat is a 1/1") {
                    game.state.projectedState.getPower(rat!!) shouldBe 1
                    game.state.projectedState.getToughness(rat) shouldBe 1
                }
            }
        }
    }
}
