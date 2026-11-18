package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Master of Predicaments (M15 #67) — {3}{U}{U} Creature — Sphinx 4/4, Flying.
 *
 *   Whenever this creature deals combat damage to a player, choose a card in your hand. That player
 *   guesses whether the card's mana value is greater than 4. If the player guessed wrong, you may
 *   cast the card without paying its mana cost.
 *
 * The controller holds no lands, so a cast that happens at all is a free one. The guess is scored
 * against the chosen card, from both sides of the 4 line, and the "may" and the uncastable land
 * (ruling) each get a run.
 */
class MasterOfPredicamentsScenarioTest : ScenarioTestBase() {

    private fun connect(vararg hand: String): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Master of Predicaments", summoningSickness = false)
            .apply { hand.forEach { withCardInHand(1, it) } }
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()

        game.declareAttackers(mapOf("Master of Predicaments" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers().error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        game.resolveStack()
        game.getLifeTotal(2) shouldBe 16
        return game
    }

    /** The controller privately chooses [card], then the damaged player answers "greater than 4?". */
    private fun TestGame.chooseAndGuess(card: String, guessGreater: Boolean) {
        withClue("the controller chooses the card: ${getPendingDecision()}") {
            getPendingDecision()?.playerId shouldBe player1Id
        }
        selectCards(listOf(findCardsInHand(1, card).first())).error shouldBe null

        val guess = getPendingDecision() as? YesNoDecision
        withClue("the damaged player guesses: ${getPendingDecision()}") {
            (guess != null) shouldBe true
            guess!!.playerId shouldBe player2Id
        }
        answerYesNo(guessGreater).error shouldBe null
    }

    init {
        test("a wrong guess lets you cast the chosen card without paying its mana cost") {
            val game = connect("Shivan Dragon", "Grizzly Bears")
            game.chooseAndGuess("Shivan Dragon", guessGreater = false)

            withClue("the cast is offered to the controller") {
                game.getPendingDecision()?.playerId shouldBe game.player1Id
            }
            game.selectCards(listOf(game.findCardsInHand(1, "Shivan Dragon").first())).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Shivan Dragon") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("a wrong guess the other way round (a low card called high) also lets you cast it") {
            val game = connect("Shivan Dragon", "Grizzly Bears")
            game.chooseAndGuess("Grizzly Bears", guessGreater = true)

            game.selectCards(listOf(game.findCardsInHand(1, "Grizzly Bears").first())).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInHand(1, "Shivan Dragon") shouldBe true
        }

        test("a right guess offers no cast and the card stays in hand") {
            val game = connect("Shivan Dragon", "Grizzly Bears")
            game.chooseAndGuess("Shivan Dragon", guessGreater = true)

            game.hasPendingDecision() shouldBe false
            game.isInHand(1, "Shivan Dragon") shouldBe true
            game.isOnBattlefield("Shivan Dragon") shouldBe false
        }

        test("after a wrong guess you may decline to cast it") {
            val game = connect("Shivan Dragon", "Grizzly Bears")
            game.chooseAndGuess("Shivan Dragon", guessGreater = false)

            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInHand(1, "Shivan Dragon") shouldBe true
            game.isOnBattlefield("Shivan Dragon") shouldBe false
        }

        test("a chosen land can't be played even if the player guessed wrong") {
            val game = connect("Island", "Grizzly Bears")
            game.chooseAndGuess("Island", guessGreater = true)

            // The guess was wrong, but a land can't be cast: the cast choice either isn't asked at
            // all or doesn't offer the Island.
            val island = game.findCardsInHand(1, "Island").first()
            val castChoice = game.getPendingDecision()
            withClue("the land is not offered as a cast: $castChoice") {
                if (castChoice != null) {
                    (castChoice is SelectCardsDecision) shouldBe true
                    (castChoice as SelectCardsDecision).options shouldNotContain island
                }
            }
            if (game.hasPendingDecision()) game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Island") shouldBe true
            game.findPermanents("Island").size shouldBe 0
        }
    }
}
