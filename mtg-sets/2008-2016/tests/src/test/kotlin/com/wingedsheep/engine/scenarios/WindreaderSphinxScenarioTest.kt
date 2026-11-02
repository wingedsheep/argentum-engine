package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Windreader Sphinx (M14 #81).
 *
 * "Whenever a creature with flying attacks, you may draw a card."
 *
 * The trigger watches every player's flyers: an opponent's flying attacker lets the Sphinx's
 * controller draw, while a non-flying attacker does nothing.
 */
class WindreaderSphinxScenarioTest : ScenarioTestBase() {

    init {
        context("Windreader Sphinx") {

            test("an opponent's flying attacker lets the Sphinx's controller draw") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Windreader Sphinx")
                    .withCardInLibrary(1, "Island")
                    .withCardOnBattlefield(2, "Wind Drake", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Wind Drake" to 1))
                game.resolveStack()

                withClue("The Sphinx's controller is asked whether to draw") {
                    game.getPendingDecision()?.playerId shouldBe game.player1Id
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Player 1 drew a card off the opponent's flyer") {
                    game.handSize(1) shouldBe 1
                }
                withClue("The attacking player drew nothing") {
                    game.handSize(2) shouldBe 0
                }
            }

            test("a non-flying attacker does not trigger the Sphinx") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Windreader Sphinx")
                    .withCardInLibrary(1, "Island")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1))
                game.resolveStack()

                withClue("No trigger, no draw") {
                    game.handSize(1) shouldBe 0
                }
            }
        }
    }
}
