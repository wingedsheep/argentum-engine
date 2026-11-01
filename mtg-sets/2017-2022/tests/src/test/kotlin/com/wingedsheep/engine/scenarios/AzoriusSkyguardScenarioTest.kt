package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Azorius Skyguard (RNA #155) — "Creatures your opponents control get -1/-0."
 */
class AzoriusSkyguardScenarioTest : ScenarioTestBase() {
    init {
        test("opponents' creatures get -1/-0, yours are untouched") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Azorius Skyguard")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val skyguard = game.findPermanent("Azorius Skyguard")!!
            projected.getPower(giant) shouldBe 2
            projected.getToughness(giant) shouldBe 3
            projected.getPower(bears) shouldBe 2
            projected.getPower(skyguard) shouldBe 3
        }
    }
}
