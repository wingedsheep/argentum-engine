package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Cloudkin Seer (M20 #54) — {2}{U} Creature — Elemental Wizard 2/1, flying.
 *
 *   When this creature enters, draw a card.
 */
class CloudkinSeerScenarioTest : ScenarioTestBase() {

    init {
        test("entering draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Cloudkin Seer")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cloudkin Seer").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Cloudkin Seer") shouldBe true
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 1
        }
    }
}
