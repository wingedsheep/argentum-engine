package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Firmament Sage (MID #53).
 *
 * "If it's neither day nor night, it becomes day as this creature enters.
 *  Whenever day becomes night or night becomes day, draw a card."
 */
class FirmamentSageScenarioTest : ScenarioTestBase() {

    init {
        context("Firmament Sage") {

            test("entering while it's neither day nor night makes it day without drawing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Firmament Sage")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state.dayNight shouldBe null

                game.castSpell(1, "Firmament Sage").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Firmament Sage") shouldBe true
                withClue("it becomes day as the Sage enters") { game.state.dayNight shouldBe DayNight.DAY }
                withClue("the first designation isn't day becoming night, so no draw") {
                    game.handSize(1) shouldBe 0
                }
            }

            test("day becoming night in the untap step draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Firmament Sage")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()
                game.state = game.state.copy(dayNight = DayNight.DAY)

                // Player 1 cast no spells this turn, so the opponent's untap step makes it night.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                game.state.dayNight shouldBe DayNight.NIGHT
                withClue("the Sage's controller draws off the change") { game.handSize(1) shouldBe 1 }
            }
        }
    }
}
