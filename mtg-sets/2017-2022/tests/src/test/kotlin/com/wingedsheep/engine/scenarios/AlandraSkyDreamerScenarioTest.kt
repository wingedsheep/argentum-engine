package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Alandra, Sky Dreamer (J22) — {2}{U}{U} Legendary Creature — Merfolk Wizard, 2/4.
 *
 *   Whenever you draw your second card each turn, create a 2/2 blue Drake creature token with flying.
 *   Whenever you draw your fifth card each turn, Alandra and Drakes you control each get +X/+X until
 *   end of turn, where X is the number of cards in your hand.
 */
class AlandraSkyDreamerScenarioTest : ScenarioTestBase() {

    init {
        context("second-draw trigger") {
            test("drawing the second card of the turn creates one 2/2 flying Drake") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Alandra, Sky Dreamer")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                val drakes = game.findPermanents("Drake Token").ifEmpty { game.findPermanents("Drake") }
                withClue("exactly one Drake token from the second draw") { drakes shouldHaveSize 1 }
                val drake = drakes.single()
                game.state.projectedState.getPower(drake) shouldBe 2
                game.state.projectedState.getToughness(drake) shouldBe 2
                game.state.projectedState.hasKeyword(drake, Keyword.FLYING) shouldBe true
            }

            test("draws past the second card don't create a Drake") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Alandra, Sky Dreamer")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                (game.findPermanents("Drake Token") + game.findPermanents("Drake")) shouldHaveSize 0
            }
        }

        context("fifth-draw trigger") {
            test("Alandra and Drakes you control get +X/+X where X is cards in hand; others don't") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Alandra, Sky Dreamer")
                    .withCardOnBattlefield(1, "Wind Drake")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withCardInHand(1, "Divination")
                    .withCardInHand(1, "Island")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    // The two Divination draws are the 4th and 5th of the turn.
                    .withCardsDrawnThisTurn(1, 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val alandra = game.findPermanent("Alandra, Sky Dreamer")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val p1 = game.player1Id
                val drakes = game.findAllPermanents("Wind Drake")
                val myDrake = drakes.single { game.state.projectedState.getController(it) == p1 }
                val theirDrake = drakes.single { it != myDrake }

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                // Hand after resolution: the Island kept in hand + the two drawn cards = 3.
                val projected = game.state.projectedState
                withClue("Alandra 2/4 + 3/+3") {
                    projected.getPower(alandra) shouldBe 5
                    projected.getToughness(alandra) shouldBe 7
                }
                withClue("your Wind Drake 2/2 + 3/+3") {
                    projected.getPower(myDrake) shouldBe 5
                    projected.getToughness(myDrake) shouldBe 5
                }
                withClue("opponent's Drake and your non-Drake are untouched") {
                    projected.getPower(theirDrake) shouldBe 2
                    projected.getPower(bears) shouldBe 2
                }
                withClue("no second-card Drake token this late in the turn") {
                    game.findPermanents("Drake Token") shouldHaveSize 0
                }
            }
        }
    }
}
