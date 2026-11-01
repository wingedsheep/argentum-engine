package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Vindictive Vampire (RNA #90) — whenever another creature you control dies, 1 damage to each
 * opponent and you gain 1 life.
 */
class VindictiveVampireScenarioTest : ScenarioTestBase() {
    init {
        test("another creature of yours dying drains each opponent for 1") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Vindictive Vampire")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }
    }
}
