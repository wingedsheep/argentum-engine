package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Skymarch Bloodletter (XLN #124) — "When this creature enters, target opponent loses 1 life and
 * you gain 1 life."
 */
class SkymarchBloodletterScenarioTest : ScenarioTestBase() {
    init {
        test("entering drains the opponent for 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Skymarch Bloodletter")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Skymarch Bloodletter").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }
    }
}
