package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Without Weakness (HOU) — target creature you control gains indestructible until end of turn; Cycling {2}.
 */
class WithoutWeaknessScenarioTest : ScenarioTestBase() {
    init {
        test("an indestructible creature survives lethal damage") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Without Weakness")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Without Weakness", bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("can't target a creature an opponent controls") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Without Weakness")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Without Weakness", game.findPermanent("Grizzly Bears")).error shouldNotBe null
        }
    }
}
