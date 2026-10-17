package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fortified Area (LEG): "Wall creatures you control get +1/+0 and have banding."
 */
class FortifiedAreaScenarioTest : ScenarioTestBase() {
    init {
        test("only Walls you control get +1/+0 and banding") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Fortified Area")
                .withCardOnBattlefield(1, "Wall of Stone")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wall of Stone")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            val (myWall, theirWall) = game.findPermanents("Wall of Stone")
                .partition { game.state.getBattlefield(game.player1Id).contains(it) }
                .let { it.first.single() to it.second.single() }
            val bears = game.findPermanent("Grizzly Bears")!!

            projected.getPower(myWall) shouldBe 1
            projected.getToughness(myWall) shouldBe 8
            projected.hasKeyword(myWall, Keyword.BANDING) shouldBe true

            projected.getPower(theirWall) shouldBe 0
            projected.hasKeyword(theirWall, Keyword.BANDING) shouldBe false
            projected.getPower(bears) shouldBe 2
            projected.hasKeyword(bears, Keyword.BANDING) shouldBe false
        }
    }
}
