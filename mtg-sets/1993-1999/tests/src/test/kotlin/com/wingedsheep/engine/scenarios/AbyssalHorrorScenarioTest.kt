package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class AbyssalHorrorScenarioTest : ScenarioTestBase() {
    init {
        test("entering targets either player and the targeted opponent chooses two cards to discard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Abyssal Horror")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Abyssal Horror").error shouldBe null
            game.resolveStack()
            val targeting = game.getPendingDecision() as ChooseTargetsDecision
            targeting.playerId shouldBe game.player1Id
            targeting.legalTargets[0]!! shouldContain game.player1Id
            targeting.legalTargets[0]!! shouldContain game.player2Id
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            val discard = game.getPendingDecision() as SelectCardsDecision
            discard.playerId shouldBe game.player2Id
            discard.minSelections shouldBe 2
            discard.maxSelections shouldBe 2
            val chosen = game.findCardsInHand(2, "Grizzly Bears") + game.findCardsInHand(2, "Forest")
            game.selectCards(chosen).error shouldBe null
            game.resolveStack()

            game.handSize(2) shouldBe 1
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Forest") shouldBe true
            game.isInHand(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Abyssal Horror") shouldBe true
        }

        test("can target its controller who discards only the one card available") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Abyssal Horror")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInHand(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Abyssal Horror").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player1Id)).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                val discard = game.getPendingDecision() as SelectCardsDecision
                discard.playerId shouldBe game.player1Id
                game.selectCards(discard.options).error shouldBe null
                game.resolveStack()
            }

            game.handSize(1) shouldBe 0
            game.isInGraveyard(1, "Forest") shouldBe true
            game.handSize(2) shouldBe 1
            game.isOnBattlefield("Abyssal Horror") shouldBe true
        }
    }
}
