package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gyre Engineer (RNA #180) — "{T}: Add {G}{U}." Both colours pay for a {G}{U} spell on their own.
 */
class GyreEngineerScenarioTest : ScenarioTestBase() {
    init {
        test("its two mana pay for a {G}{U} spell") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Gyre Engineer")
                .withCardInHand(1, "Gyre Engineer")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gyre Engineer").error shouldBe null
            game.resolveStack()

            game.findPermanents("Gyre Engineer").size shouldBe 2
        }
    }
}
