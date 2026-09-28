package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class LiuBeiLordOfShuScenarioTest : ScenarioTestBase() {
    private fun powerToughness(other: String?, controller: Int = 1): Pair<Int?, Int?> {
        val b = scenario()
            .withPlayers("A", "B")
            .withCardOnBattlefield(1, "Liu Bei, Lord of Shu")
        if (other != null) b.withCardOnBattlefield(controller, other)
        val game = b.withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        val id = game.findPermanent("Liu Bei, Lord of Shu")!!
        val p = game.state.projectedState
        return p.getPower(id) to p.getToughness(id)
    }

    init {
        context("Liu Bei, Lord of Shu") {
            test("2/4 alone") { powerToughness(null) shouldBe (2 to 4) }
            test("4/6 with Guan Yu") { powerToughness("Guan Yu, Sainted Warrior") shouldBe (4 to 6) }
            test("4/6 with Zhang Fei") { powerToughness("Zhang Fei, Fierce Warrior") shouldBe (4 to 6) }
            test("opponent's Guan Yu does not count") { powerToughness("Guan Yu, Sainted Warrior", 2) shouldBe (2 to 4) }
        }
    }
}
