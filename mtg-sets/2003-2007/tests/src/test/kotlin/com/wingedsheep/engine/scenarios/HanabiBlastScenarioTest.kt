package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class HanabiBlastScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")

        test("deals 2 to a player, returns to hand, then the only card in hand is discarded at random") {
            val game = base().withCardInHand(1, "Hanabi Blast").build()
            game.castSpellTargetingPlayer(1, "Hanabi Blast", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            // Returned to hand first, so it is the one card the random discard can hit.
            game.handSize(1) shouldBe 0
            game.isInGraveyard(1, "Hanabi Blast") shouldBe true
        }

        test("kills a creature and returns to hand before a random discard of one card") {
            val game = base().withCardInHand(1, "Hanabi Blast")
                .withCardInHand(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears").build()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Hanabi Blast", bear).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            // Hand was {Bears} + returned Blast = 2; one discarded at random leaves 1.
            game.handSize(1) shouldBe 1
            val inHand = game.isInHand(1, "Hanabi Blast")
            game.isInGraveyard(1, "Hanabi Blast") shouldBe !inHand
            game.isInGraveyard(1, "Grizzly Bears") shouldBe inHand
        }
    }
}
