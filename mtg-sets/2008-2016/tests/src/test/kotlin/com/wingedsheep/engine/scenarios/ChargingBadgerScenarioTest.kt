package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Charging Badger (BNG #118) — a 1/1 for {G} with trample.
 */
class ChargingBadgerScenarioTest : ScenarioTestBase() {

    init {
        test("is a 1/1 with trample") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Charging Badger")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val badger = game.findPermanent("Charging Badger")!!
            val projected = game.state.projectedState
            projected.getPower(badger) shouldBe 1
            projected.getToughness(badger) shouldBe 1
            projected.hasKeyword(badger, Keyword.TRAMPLE) shouldBe true
        }
    }
}
