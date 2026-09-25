package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Keiga, the Tide Star (CHK #72) — "When Keiga dies, gain control of target creature."
 */
class KeigaTheTideStarScenarioTest : ScenarioTestBase() {

    init {
        context("Keiga, the Tide Star") {

            test("dying steals the targeted creature, and the control change outlasts the turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Keiga, the Tide Star")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val keiga = game.findPermanent("Keiga, the Tide Star")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Murder", keiga).error shouldBe null
                game.resolveStack()

                withClue("Keiga died") { game.isInGraveyard(1, "Keiga, the Tide Star") shouldBe true }
                withClue("its dies trigger asks for a target") {
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                game.resolveStack()

                withClue("Player1 now controls the Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the effect has no duration — it survives cleanup into the next turn") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
            }
        }
    }
}
