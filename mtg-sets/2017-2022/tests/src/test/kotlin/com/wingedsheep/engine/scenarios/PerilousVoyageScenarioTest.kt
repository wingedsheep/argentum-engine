package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Perilous Voyage (XLN #67).
 *
 * Perilous Voyage — {1}{U} Instant.
 *   "Return target nonland permanent you don't control to its owner's hand. If its mana value
 *    was 2 or less, scry 2."
 */
class PerilousVoyageScenarioTest : ScenarioTestBase() {

    init {
        context("Perilous Voyage") {

            fun buildGame(opponentPermanent: String) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Perilous Voyage")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, opponentPermanent)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("bouncing a permanent with mana value 2 or less scries 2") {
                val game = buildGame("Grizzly Bears")
                val bears = game.findPermanents("Grizzly Bears").first {
                    game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player2Id
                }

                game.castSpell(1, "Perilous Voyage", bears).error shouldBe null
                game.resolveStack()

                withClue("The opponent's Grizzly Bears returned to its owner's hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
                val decision = game.getPendingDecision()
                withClue("Mana value 2 triggers scry 2 over the top two cards") {
                    (decision is SelectCardsDecision) shouldBe true
                    (decision as SelectCardsDecision).options.size shouldBe 2
                }
                game.selectCards((decision as SelectCardsDecision).options).error shouldBe null
                game.resolveStack()

                withClue("Both looked-at cards went to the bottom; the third card surfaces") {
                    game.librarySize(1) shouldBe 3
                    game.state.getEntity(game.state.getLibrary(game.player1Id).first())
                        ?.get<CardComponent>()?.name shouldBe "Island"
                }
            }

            test("bouncing a permanent with mana value 3 or more does not scry") {
                val game = buildGame("Hill Giant")
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Perilous Voyage", giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant returned to its owner's hand") {
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
                withClue("Mana value 4 is above the threshold — no scry decision") {
                    game.getPendingDecision() shouldBe null
                }
                withClue("Library untouched") {
                    game.librarySize(1) shouldBe 3
                }
            }

            test("cannot target a land or a permanent you control") {
                val game = buildGame("Hill Giant")
                val forest = game.findPermanent("Forest")!!
                val ownBears = game.findPermanent("Grizzly Bears")!!

                withClue("An opponent's land is not a nonland permanent") {
                    game.castSpell(1, "Perilous Voyage", forest).error shouldNotBe null
                }
                withClue("Your own permanent is not a legal target") {
                    game.castSpell(1, "Perilous Voyage", ownBears).error shouldNotBe null
                }
            }
        }
    }
}
