package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class DrogskolCaptainScenarioTest : ScenarioTestBase() {
    init {
        test("other Spirits you control get +1/+1 and hexproof; the Captain and non-Spirits don't") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Drogskol Captain")
                .withCardOnBattlefield(1, "Niblis of the Mist")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Niblis of the Urn")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val projected = game.state.projectedState
            val captain = game.findPermanent("Drogskol Captain")!!
            val mist = game.findPermanent("Niblis of the Mist")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val enemySpirit = game.findPermanent("Niblis of the Urn")!!

            projected.getPower(mist) shouldBe 3
            projected.getToughness(mist) shouldBe 2
            projected.hasKeyword(mist, Keyword.HEXPROOF) shouldBe true

            projected.getPower(captain) shouldBe 2
            projected.hasKeyword(captain, Keyword.HEXPROOF) shouldBe false
            projected.getPower(bears) shouldBe 2
            projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe false
            projected.getPower(enemySpirit) shouldBe 1
            projected.hasKeyword(enemySpirit, Keyword.HEXPROOF) shouldBe false
        }
    }
}
