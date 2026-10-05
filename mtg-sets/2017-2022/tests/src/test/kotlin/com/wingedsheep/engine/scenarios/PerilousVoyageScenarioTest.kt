package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.PiratedCopy
import com.wingedsheep.mtg.sets.definitions.xln.cards.PerilousVoyage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.types.shouldBeInstanceOf
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

            test("uses the mana value the permanent had on the battlefield — a bounced copy of a 2-drop scries") {
                val d = GameTestDriver()
                d.registerCards(TestCards.all + listOf(PiratedCopy, PerilousVoyage))
                d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
                d.passPriorityUntil(Step.PRECOMBAT_MAIN)

                // Player 1's Pirated Copy ({4}{U}, mana value 5 in hand) enters as a copy of Grizzly Bears.
                val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
                val copy = d.putCardInHand(d.player1, "Pirated Copy")
                d.giveMana(d.player1, Color.BLUE, 5)
                d.castSpell(d.player1, copy).error shouldBe null
                d.bothPass()
                d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                d.submitCardSelection(d.player1, listOf(bears)).error shouldBe null

                // Player 2 bounces the copy at instant speed.
                d.passPriority(d.player1)
                val voyage = d.putCardInHand(d.player2, "Perilous Voyage")
                d.giveMana(d.player2, Color.BLUE, 2)
                d.castSpell(d.player2, voyage, listOf(copy)).error shouldBe null
                d.bothPass()

                withClue("The copy is back in its owner's hand") {
                    d.getHand(d.player1).contains(copy) shouldBe true
                }
                withClue("It had mana value 2 on the battlefield, so its caster scries 2") {
                    val scry = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                    scry.playerId shouldBe d.player2
                    scry.options.size shouldBe 2
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
