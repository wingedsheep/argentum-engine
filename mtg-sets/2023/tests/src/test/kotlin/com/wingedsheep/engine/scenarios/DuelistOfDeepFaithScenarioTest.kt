package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Duelist of Deep Faith (ONE #9) — {1}{W} 2/2 Phyrexian Soldier.
 * "Toxic 1. During your turn, this creature has first strike."
 */
class DuelistOfDeepFaithScenarioTest : ScenarioTestBase() {
    init {
        test("has toxic and first strike during its controller's turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Duelist of Deep Faith")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val duelist = game.findPermanent("Duelist of Deep Faith")!!
            val projected = game.state.projectedState
            projected.hasKeyword(duelist, Keyword.TOXIC) shouldBe true
            projected.hasKeyword(duelist, Keyword.FIRST_STRIKE) shouldBe true
        }

        test("lacks first strike during an opponent's turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Duelist of Deep Faith")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val duelist = game.findPermanent("Duelist of Deep Faith")!!
            val projected = game.state.projectedState
            projected.hasKeyword(duelist, Keyword.TOXIC) shouldBe true
            projected.hasKeyword(duelist, Keyword.FIRST_STRIKE) shouldBe false
        }
    }
}
