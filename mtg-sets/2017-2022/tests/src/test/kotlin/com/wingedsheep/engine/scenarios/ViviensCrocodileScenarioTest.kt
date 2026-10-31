package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Vivien's Crocodile (M20 #301) — {2}{G} 3/3 Creature — Crocodile Spirit.
 *
 *   This creature gets +1/+1 as long as you control a Vivien planeswalker.
 */
class ViviensCrocodileScenarioTest : ScenarioTestBase() {

    init {
        context("Vivien's Crocodile") {
            test("is a 3/3 without a Vivien") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vivien's Crocodile")
                    .withCardOnBattlefield(2, "Vivien Reid")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val croc = game.findPermanent("Vivien's Crocodile")!!
                game.state.projectedState.getPower(croc) shouldBe 3
                game.state.projectedState.getToughness(croc) shouldBe 3
            }

            test("is a 4/4 while you control a Vivien planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vivien's Crocodile")
                    .withCardOnBattlefield(1, "Vivien Reid")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val croc = game.findPermanent("Vivien's Crocodile")!!
                game.state.projectedState.getPower(croc) shouldBe 4
                game.state.projectedState.getToughness(croc) shouldBe 4
            }
        }
    }
}
