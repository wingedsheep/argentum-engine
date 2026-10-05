package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Steelgaze Griffin (ELD #65) — {4}{U} 2/4 Griffin.
 * Flying
 * "Whenever you draw your second card each turn, this creature gets +2/+0 until end of turn."
 */
class SteelgazeGriffinScenarioTest : ScenarioTestBase() {

    private fun powerAndToughness(game: TestGame): Pair<Int?, Int?> {
        val id = game.findPermanent("Steelgaze Griffin") ?: error("Steelgaze Griffin not on battlefield")
        val projected = StateProjector().project(game.state)
        return projected.getPower(id) to projected.getToughness(id)
    }

    init {
        context("Steelgaze Griffin's second-draw trigger") {

            test("the first draw doesn't trigger, the second gives +2/+0 once, the third doesn't stack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Steelgaze Griffin")
                    .withCardInHand(1, "Think Twice")
                    .withCardInHand(1, "Think Twice")
                    .withCardInHand(1, "Think Twice")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                powerAndToughness(game) shouldBe (2 to 4)

                game.castSpell(1, "Think Twice").error shouldBe null
                game.resolveStack()
                withClue("the first draw of the turn should not trigger") {
                    powerAndToughness(game) shouldBe (2 to 4)
                }

                game.castSpell(1, "Think Twice").error shouldBe null
                game.resolveStack()
                withClue("the second draw of the turn should give +2/+0") {
                    powerAndToughness(game) shouldBe (4 to 4)
                }

                game.castSpell(1, "Think Twice").error shouldBe null
                game.resolveStack()
                withClue("the third draw of the turn should not trigger again") {
                    powerAndToughness(game) shouldBe (4 to 4)
                }

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                withClue("the bonus should wear off at end of turn") {
                    powerAndToughness(game) shouldBe (2 to 4)
                }
            }

            test("drawing two at once triggers on the second card only once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Steelgaze Griffin")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                powerAndToughness(game) shouldBe (4 to 4)
            }
        }
    }
}
