package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wiretapping (SNC #65) — {4}{U} Enchantment.
 *
 *   Hideaway 5
 *   Whenever you draw your first card during each of your draw steps, draw a card. Then if you
 *   have nine or more cards in hand, you may play the exiled card without paying its mana cost.
 *
 * Covers: the trigger fires on your own draw step only (not the opponent's), draws exactly one
 * extra card (its own draw doesn't retrigger it), offers no free play below nine cards, and at
 * nine or more lets you play the hidden card for free.
 */
class WiretappingScenarioTest : ScenarioTestBase() {

    init {
        context("Wiretapping") {

            test("draws one extra card on your draw step; below nine cards there's no free-play offer") {
                val game = wiretappingGame(extraHandCards = 2)
                resolveHideaway(game)

                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                withClue("the opponent's draw step doesn't fire it") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.handSize(1) shouldBe 2
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                withClue("turn-based draw plus Wiretapping's one card") {
                    game.handSize(1) shouldBe 4
                }
                withClue("four cards in hand: no offer, the hidden card stays exiled") {
                    game.getPendingDecision() shouldBe null
                    game.state.getExile(game.player1Id).size shouldBe 1
                }
            }

            test("with nine or more cards in hand, you may play the exiled card for free") {
                val game = wiretappingGame(extraHandCards = 7)
                resolveHideaway(game)

                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                withClue("seven + the draw + Wiretapping's draw = nine: the free play is offered") {
                    game.handSize(1) shouldBe 9
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }

                game.answerYesNo(true)
                game.resolveStack()

                withClue("Grizzly Bears is cast from exile during the draw step without paying") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                    game.state.getExile(game.player1Id) shouldBe emptyList()
                }
            }
        }
    }

    private fun wiretappingGame(extraHandCards: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Wiretapping")
            .withLandsOnBattlefield(1, "Island", 5)
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Sol Ring")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(extraHandCards) { builder.withCardInHand(1, "Island") }
        repeat(5) { builder.withCardInLibrary(1, "Island") }
        repeat(5) { builder.withCardInLibrary(2, "Island") }
        return builder.build()
    }

    /** Cast Wiretapping and resolve Hideaway, exiling the top card (Grizzly Bears). */
    private fun resolveHideaway(game: TestGame) {
        game.castSpell(1, "Wiretapping").error shouldBe null
        game.resolveStack()
        val decision = game.getPendingDecision() as? SelectCardsDecision
            ?: error("expected a hideaway selection, got ${game.getPendingDecision()}")
        game.selectCards(listOf(decision.options.first()))
    }
}
