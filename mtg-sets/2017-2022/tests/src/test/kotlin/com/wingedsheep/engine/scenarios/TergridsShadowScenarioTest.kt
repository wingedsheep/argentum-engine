package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TergridsShadowScenarioTest : ScenarioTestBase() {
    init {
        test("each player chooses two in APNAP order, then all chosen creatures die together") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Tergrid's Shadow")
                .withCardOnBattlefield(1, "Blood Artist")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Raging Goblin")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Tergrid's Shadow").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe game.player1Id
            game.selectCards(listOf(game.findPermanent("Blood Artist")!!, game.findPermanent("Grizzly Bears")!!)).error shouldBe null

            // Nothing is sacrificed until the opponent has chosen too.
            game.isOnBattlefield("Blood Artist") shouldBe true
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe game.player2Id
            val giants = game.findPermanents("Hill Giant")
            game.selectCards(giants).error shouldBe null

            game.isInGraveyard(1, "Blood Artist") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Raging Goblin") shouldBe true
            game.isOnBattlefield("Craw Wurm") shouldBe true
            game.findPermanents("Hill Giant").size shouldBe 0

            // Blood Artist died alongside the other three, so it sees all four deaths.
            var drains = 0
            while (true) {
                if (game.getPendingDecision() !is ChooseTargetsDecision) break
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                drains++
                game.resolveStack()
            }
            drains shouldBe 4
        }

        test("a player with two or fewer creatures sacrifices them all without a prompt") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Tergrid's Shadow")
                .withCardOnBattlefield(1, "Raging Goblin")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Tergrid's Shadow").error shouldBe null
            game.resolveStack()

            (game.getPendingDecision() is SelectCardsDecision) shouldBe false
            game.isInGraveyard(1, "Raging Goblin") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
