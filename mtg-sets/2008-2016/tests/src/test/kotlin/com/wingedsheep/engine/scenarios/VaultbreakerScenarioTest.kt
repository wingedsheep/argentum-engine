package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vaultbreaker (FRF #117) — {3}{R} Creature — Orc Rogue 4/2.
 *
 *   Whenever this creature attacks, you may discard a card. If you do, draw a card.
 *   Dash {2}{R}
 *
 * The draw is gated on the discard actually happening, so an empty hand draws nothing.
 */
class VaultbreakerScenarioTest : ScenarioTestBase() {

    private fun attack(handCards: List<String>): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Vaultbreaker")
            .withCardInLibrary(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        handCards.forEach { builder = builder.withCardInHand(1, it) }
        val game = builder.build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Vaultbreaker" to 2)).error shouldBe null
        var guard = 0
        while (game.state.pendingDecision == null && game.state.stack.isNotEmpty() && guard++ < 20) {
            game.resolveStack()
        }
        return game
    }

    init {
        context("Vaultbreaker") {

            test("attacking lets you discard a card to draw a card") {
                val game = attack(listOf("Mountain", "Forest"))
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                val mountain = game.findCardsInHand(1, "Mountain").single()
                game.selectCards(listOf(mountain)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Mountain") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
                withClue("the discard paid for the draw") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.handSize(1) shouldBe 2
                }
            }

            test("declining keeps the hand and draws nothing") {
                val game = attack(listOf("Mountain", "Forest"))
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 2
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }

            test("with an empty hand there is nothing to discard, so nothing is drawn") {
                val game = attack(emptyList())
                if (game.getPendingDecision() is YesNoDecision) game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 0
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }
        }
    }
}
