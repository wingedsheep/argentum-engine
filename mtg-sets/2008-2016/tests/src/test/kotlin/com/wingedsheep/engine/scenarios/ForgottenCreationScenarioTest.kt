package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Forgotten Creation (SOI #63).
 *
 * "At the beginning of your upkeep, you may discard all the cards in your hand. If you do, draw
 *  that many cards."
 *
 * What's worth proving is the count: "that many" is the number of cards actually discarded, read
 * from the discarded-hand collection the "if you do" gate carries into its draw — so an empty
 * hand draws nothing, and declining keeps the hand untouched.
 */
class ForgottenCreationScenarioTest : ScenarioTestBase() {

    /** Player 1 controls Forgotten Creation; it is Player 2's turn so we can pass into P1's upkeep. */
    private fun board(handCards: Int) = run {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Forgotten Creation", summoningSickness = false)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(handCards) { builder = builder.withCardInHand(1, "Mountain") }
        repeat(6) { builder = builder.withCardInLibrary(1, "Grizzly Bears") }
        repeat(6) { builder = builder.withCardInLibrary(2, "Grizzly Bears") }
        builder.build()
    }

    init {
        context("Forgotten Creation upkeep trigger") {

            test("discarding a hand of three draws exactly three") {
                val game = board(handCards = 3)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.resolveStack()

                withClue("all three Mountains were discarded") {
                    game.graveyardSize(1) shouldBe 3
                    game.isInHand(1, "Mountain") shouldBe false
                }
                withClue("three fresh cards replaced them") {
                    game.handSize(1) shouldBe 3
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 3
                }
            }

            test("declining keeps the hand and draws nothing") {
                val game = board(handCards = 2)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                game.handSize(1) shouldBe 2
                game.findCardsInHand(1, "Mountain").size shouldBe 2
                game.graveyardSize(1) shouldBe 0
            }

            test("an empty hand discards nothing and draws nothing") {
                val game = board(handCards = 0)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                if (game.hasPendingDecision()) game.answerYesNo(true)
                game.resolveStack()

                withClue("zero cards discarded means zero cards drawn") {
                    game.handSize(1) shouldBe 0
                    game.graveyardSize(1) shouldBe 0
                }
            }
        }
    }
}
