package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fevered Visions (SOI #244) — {1}{U}{R} Enchantment
 *
 *   At the beginning of each player's end step, that player draws a card. If the player is your
 *   opponent and has four or more cards in hand, this enchantment deals 2 damage to that player.
 *
 * The hand-size check happens after the draw (ruling 2016-04-08), and only an opponent is hurt.
 */
class FeveredVisionsScenarioTest : ScenarioTestBase() {

    private fun build(activePlayer: Int, opponentHand: Int, controllerHand: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Fevered Visions")
        .apply { repeat(controllerHand) { withCardInHand(1, "Forest") } }
        .apply { repeat(opponentHand) { withCardInHand(2, "Forest") } }
        .apply { repeat(5) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
        .withActivePlayer(activePlayer)
        .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        .build()

    init {
        context("Fevered Visions") {

            test("an opponent who reaches four cards after the draw takes 2 damage") {
                val game = build(activePlayer = 2, opponentHand = 3, controllerHand = 0)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the opponent drew at their end step") { game.handSize(2) shouldBe 4 }
                withClue("four cards after the draw → 2 damage") { game.getLifeTotal(2) shouldBe 18 }
            }

            test("an opponent left with three cards after the draw takes no damage") {
                val game = build(activePlayer = 2, opponentHand = 2, controllerHand = 0)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.handSize(2) shouldBe 3
                game.getLifeTotal(2) shouldBe 20
            }

            test("the controller draws at their own end step but is never damaged") {
                val game = build(activePlayer = 1, opponentHand = 0, controllerHand = 5)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.handSize(1) shouldBe 6
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
