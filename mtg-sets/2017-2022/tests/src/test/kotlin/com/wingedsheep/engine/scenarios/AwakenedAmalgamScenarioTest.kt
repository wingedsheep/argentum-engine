package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Awakened Amalgam (RIX #175) — "power and toughness are each equal to the number of differently
 * named lands you control." First CDA over the distinct-names battlefield aggregation: duplicates
 * collapse to one, and the opponent's lands don't count.
 */
class AwakenedAmalgamScenarioTest : ScenarioTestBase() {
    init {
        test("counts each land name you control once, ignoring the opponent's lands") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Awakened Amalgam")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(2, "Forest", 3)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val amalgam = game.findPermanent("Awakened Amalgam")!!
            val projected = game.state.projectedState
            projected.getPower(amalgam) shouldBe 3
            projected.getToughness(amalgam) shouldBe 3
        }
    }
}
