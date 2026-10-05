package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class VendilionCliqueScenarioTest : ScenarioTestBase() {
    init {
        /** Player 1 flashes in the Clique during the opponent's main phase, targeting [targetPlayer]. */
        fun castClique(targetPlayer: Int): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Vendilion Clique")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Island")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Vendilion Clique").error shouldBe null
            game.resolveStack()
            val target = if (targetPlayer == 1) game.player1Id else game.player2Id
            game.selectTargets(listOf(target)).error shouldBe null
            game.resolveStack()
            return game
        }

        test("flashed in on the opponent's turn: chosen card goes to the bottom and its owner draws") {
            val game = castClique(targetPlayer = 2)
            game.isOnBattlefield("Vendilion Clique") shouldBe true
            val decision = game.getPendingDecision() as SelectCardsDecision
            val bears = game.findCardsInHand(2, "Grizzly Bears").single()
            val island = game.findCardsInHand(2, "Island").single()
            decision.options.contains(island) shouldBe false
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Grizzly Bears") shouldBe false
            game.state.getLibrary(game.player2Id).last() shouldBe bears
            game.isInHand(2, "Forest") shouldBe true
            game.isInHand(2, "Island") shouldBe true
            game.handSize(2) shouldBe 2
            game.handSize(1) shouldBe 1
        }

        test("choosing nothing leaves the hand and library untouched") {
            val game = castClique(targetPlayer = 2)
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.isInHand(2, "Forest") shouldBe false
            game.handSize(2) shouldBe 2
            game.librarySize(2) shouldBe 1
        }

        test("can target its controller to cycle away their own card") {
            val game = castClique(targetPlayer = 1)
            val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
            game.selectCards(listOf(bolt)).error shouldBe null
            game.resolveStack()

            game.state.getLibrary(game.player1Id).last() shouldBe bolt
            game.isInHand(1, "Mountain") shouldBe true
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.handSize(2) shouldBe 2
        }
    }
}
