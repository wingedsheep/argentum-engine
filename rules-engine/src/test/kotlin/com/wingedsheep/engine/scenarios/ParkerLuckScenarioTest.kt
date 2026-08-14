package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Parker Luck (Marvel's Spider-Man, {2}{B} Enchantment).
 *
 *   "At the beginning of your end step, two target players each reveal the top card of
 *    their library. They each lose life equal to the mana value of the card revealed by
 *    the other player. Then they each put the card they revealed into their hand."
 *
 * The critical clause is the *cross-referenced* life loss: each target loses life equal to
 * the mana value of the card the OTHER target revealed. Distinct mana values on the two top
 * cards (Grizzly Bears mv 2, Serra Angel mv 5) prove the swap: the player who revealed the
 * mv-2 card loses 5, and the player who revealed the mv-5 card loses 2.
 */
class ParkerLuckScenarioTest : ScenarioTestBase() {

    init {
        test("both targets reveal, lose life equal to the other's card, then draw the revealed card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Parker Luck")
                .withCardInLibrary(1, "Grizzly Bears") // mana value 2 ({1}{G})
                .withCardInLibrary(2, "Serra Angel")   // mana value 5 ({3}{W}{W})
                .build()

            // Advance to the controller's end step so the trigger fires.
            game.passUntilPhase(Phase.ENDING, Step.END)
            // Placing the trigger on the stack asks the controller to choose two target players.
            game.resolveStack()
            game.selectTargets(listOf(game.player1Id, game.player2Id))
            game.resolveStack()

            withClue("Player 1 revealed Grizzly Bears (mv 2), so loses life equal to Player 2's Serra Angel (mv 5)") {
                game.getLifeTotal(1) shouldBe 15
            }
            withClue("Player 2 revealed Serra Angel (mv 5), so loses life equal to Player 1's Grizzly Bears (mv 2)") {
                game.getLifeTotal(2) shouldBe 18
            }
            withClue("Player 1 puts their revealed Grizzly Bears into hand") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
            withClue("Player 2 puts their revealed Serra Angel into hand") {
                game.isInHand(2, "Serra Angel") shouldBe true
            }
        }
    }
}
