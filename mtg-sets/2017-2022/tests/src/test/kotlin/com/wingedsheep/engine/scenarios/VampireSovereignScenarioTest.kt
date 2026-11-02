package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Vampire Sovereign (M19 #125) — "When this creature enters, target opponent loses 3 life and you
 * gain 3 life."
 */
class VampireSovereignScenarioTest : ScenarioTestBase() {
    init {
        test("entering drains the opponent for 3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Vampire Sovereign")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Vampire Sovereign").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
        }
    }
}
