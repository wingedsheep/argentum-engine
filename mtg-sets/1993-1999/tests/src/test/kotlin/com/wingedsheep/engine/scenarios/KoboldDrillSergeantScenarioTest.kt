package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Kobold Drill Sergeant (LEG): "Other Kobold creatures you control get +0/+1 and have trample."
 */
class KoboldDrillSergeantScenarioTest : ScenarioTestBase() {
    init {
        test("other Kobolds you control get +0/+1 and trample; the Sergeant itself does not") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kobold Drill Sergeant")
                .withCardOnBattlefield(1, "Kobolds of Kher Keep")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            val sergeant = game.findPermanent("Kobold Drill Sergeant")!!
            val kobolds = game.findPermanent("Kobolds of Kher Keep")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            projected.getToughness(kobolds) shouldBe 2
            projected.hasKeyword(kobolds, Keyword.TRAMPLE) shouldBe true
            projected.getToughness(sergeant) shouldBe 2
            projected.hasKeyword(sergeant, Keyword.TRAMPLE) shouldBe false
            projected.getToughness(bears) shouldBe 2
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
        }
    }
}
