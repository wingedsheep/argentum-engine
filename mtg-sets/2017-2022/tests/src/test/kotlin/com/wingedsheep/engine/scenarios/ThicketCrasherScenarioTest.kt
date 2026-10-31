package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Thicket Crasher (M20 #196) — {3}{G} 4/3 Creature — Elemental Rhino.
 *
 *   Trample
 *   Other Elementals you control have trample.
 */
class ThicketCrasherScenarioTest : ScenarioTestBase() {

    init {
        context("Thicket Crasher") {
            test("grants trample to other Elementals you control only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thicket Crasher")
                    .withCardOnBattlefield(1, "Air Elemental")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Fire Elemental")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p = game.state.projectedState
                p.hasKeyword(game.findPermanent("Thicket Crasher")!!, Keyword.TRAMPLE) shouldBe true
                p.hasKeyword(game.findPermanent("Air Elemental")!!, Keyword.TRAMPLE) shouldBe true
                p.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.TRAMPLE) shouldBe false
                p.hasKeyword(game.findPermanent("Fire Elemental")!!, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
