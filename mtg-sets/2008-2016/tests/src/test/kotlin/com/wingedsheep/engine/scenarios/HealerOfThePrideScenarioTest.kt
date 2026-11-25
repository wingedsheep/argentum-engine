package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Healer of the Pride (M13 #19) — {3}{W} 2/3 Creature — Cat Cleric.
 *   Whenever another creature you control enters, you gain 2 life.
 */
class HealerOfThePrideScenarioTest : ScenarioTestBase() {
    init {
        test("gains 2 life when another creature you control enters, but not for itself") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Healer of the Pride")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Healer of the Pride").error shouldBe null
            game.resolveStack()
            withClue("Healer entering doesn't trigger itself") { game.getLifeTotal(1) shouldBe 20 }

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("another creature you control entering gains 2 life") { game.getLifeTotal(1) shouldBe 22 }
        }
    }
}
