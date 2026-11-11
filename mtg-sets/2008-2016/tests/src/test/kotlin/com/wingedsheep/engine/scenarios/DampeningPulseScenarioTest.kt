package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Dampening Pulse — only opponents' creatures get -1/-0. */
class DampeningPulseScenarioTest : ScenarioTestBase() {
    init {
        test("opponents' creatures get -1/-0, yours are unaffected") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dampening Pulse")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val projected = game.state.projectedState
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            projected.getPower(bears) shouldBe 2
            projected.getPower(giant) shouldBe 2
            projected.getToughness(giant) shouldBe 3
        }
    }
}
