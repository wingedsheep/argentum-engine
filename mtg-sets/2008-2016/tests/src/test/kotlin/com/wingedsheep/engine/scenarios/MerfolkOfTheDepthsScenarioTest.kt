package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Merfolk of the Depths (GTC) — 4/2 with flash. */
class MerfolkOfTheDepthsScenarioTest : ScenarioTestBase() {
    init {
        test("can be cast on the opponent's turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Merfolk of the Depths")
                .withLandsOnBattlefield(1, "Island", 6)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Merfolk of the Depths").error shouldBe null
            game.resolveStack()

            val merfolk = game.findPermanent("Merfolk of the Depths")!!
            game.state.projectedState.getPower(merfolk) shouldBe 4
            game.state.projectedState.getToughness(merfolk) shouldBe 2
        }
    }
}
