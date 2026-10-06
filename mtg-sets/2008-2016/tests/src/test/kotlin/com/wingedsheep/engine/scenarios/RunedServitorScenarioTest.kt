package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe

class RunedServitorScenarioTest : ScenarioTestBase() {
    init {
        test("dying draws one card for both players after the source leaves") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Runed Servitor")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .build()

            val servitor = game.findPermanent("Runed Servitor")!!
            game.castSpell(1, "Lightning Bolt", servitor).error shouldBe null
            game.resolveStack()

            game.findPermanent("Runed Servitor") shouldBe null
            game.handSize(1) shouldBe 1
            game.handSize(2) shouldBe 1
            game.hasPendingDecision() shouldBe false
        }

        test("exiling the servitor does not trigger its death ability") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Runed Servitor")
                .withCardInHand(1, "Swords to Plowshares")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .build()

            val servitor = game.findPermanent("Runed Servitor")!!
            game.castSpell(1, "Swords to Plowshares", servitor).error shouldBe null
            game.resolveStack()

            game.findPermanent("Runed Servitor") shouldBe null
            game.handSize(1) shouldBe 0
            game.handSize(2) shouldBe 0
        }
    }
}
