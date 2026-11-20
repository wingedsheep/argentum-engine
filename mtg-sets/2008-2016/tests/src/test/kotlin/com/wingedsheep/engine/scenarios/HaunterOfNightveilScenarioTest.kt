package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Haunter of Nightveil (DGM #76) — {3}{U}{B} 3/4 Creature — Spirit.
 *
 *   Creatures your opponents control get -1/-0.
 */
class HaunterOfNightveilScenarioTest : ScenarioTestBase() {
    init {
        test("shrinks only the opponent's creatures by -1/-0") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Haunter of Nightveil")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val haunter = game.findPermanent("Haunter of Nightveil")!!

            projected.getPower(giant) shouldBe 2
            projected.getToughness(giant) shouldBe 3
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 2
            projected.getPower(haunter) shouldBe 3
        }
    }
}
