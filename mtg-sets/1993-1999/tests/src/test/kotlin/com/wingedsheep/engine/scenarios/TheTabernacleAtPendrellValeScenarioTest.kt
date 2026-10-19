package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Tabernacle at Pendrell Vale (LEG #307).
 *
 * "All creatures have 'At the beginning of your upkeep, destroy this creature unless you pay {1}.'"
 *
 * Pinned: the granted trigger reaches creatures the Tabernacle's controller doesn't control, it is
 * the *creature's* controller who pays (on their own upkeep), and the Tabernacle controller's
 * creatures don't trigger on the opponent's upkeep.
 */
class TheTabernacleAtPendrellValeScenarioTest : ScenarioTestBase() {

    /** Player 1 controls Grizzly Bears and a Forest; Player 2 controls the Tabernacle and Hill Giant. */
    private fun atPlayer1Upkeep(): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withCardOnBattlefield(2, "The Tabernacle at Pendrell Vale")
            .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
        val game = builder.build()
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        context("The Tabernacle at Pendrell Vale upkeep tax") {

            test("the opponent's creature's controller is asked, and declining destroys it") {
                val game = atPlayer1Upkeep()

                val decision = game.getPendingDecision()
                withClue("Grizzly Bears' controller decides whether to pay {1}") {
                    (decision != null) shouldBe true
                    decision!!.playerId shouldBe game.player1Id
                }
                game.answerYesNo(false)
                game.resolveStack()

                withClue("unpaid, Grizzly Bears is destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("the Tabernacle controller's creature doesn't trigger on the opponent's upkeep") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.getPendingDecision() shouldBe null
                }
            }

            test("paying {1} keeps the creature") {
                val game = atPlayer1Upkeep()

                game.answerYesNo(true)
                game.resolveStack()

                withClue("Grizzly Bears survives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant is untouched") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }
    }
}
