package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Plundering Predator (J22 #37) — {4}{R} Creature — Dragon, 3/3, Flying.
 *
 *   When this creature enters, you may discard a card. If you do, draw a card.
 *
 * The draw is gated on the discard actually happening — declining the "may" draws nothing.
 */
class PlunderingPredatorScenarioTest : ScenarioTestBase() {

    init {
        context("Plundering Predator") {

            test("accepting discards a card and then draws one") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Plundering Predator")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Plundering Predator").error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears was discarded and Hill Giant drawn") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Hill Giant") shouldBe true
                    game.handSize(1) shouldBe 1
                }
            }

            test("with nothing to discard, accepting draws nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Plundering Predator")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Plundering Predator").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.answerYesNo(true).error shouldBe null
                }
                game.resolveStack()

                withClue("no card was discarded, so no card is drawn") {
                    game.handSize(1) shouldBe 0
                    game.librarySize(1) shouldBe 1
                }
            }

            test("declining neither discards nor draws") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Plundering Predator")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Plundering Predator").error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("hand and library are untouched") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Hill Giant") shouldBe false
                    game.librarySize(1) shouldBe 1
                }
            }
        }
    }
}
