package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Granitic Titan (HOU) — {4}{R}{R} 5/4 menace, cycling {2}.
 */
class GraniticTitanScenarioTest : ScenarioTestBase() {
    init {
        test("has menace") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Granitic Titan")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val creature = game.findPermanent("Granitic Titan")!!
            game.state.projectedState.hasKeyword(creature, Keyword.MENACE) shouldBe true
        }

        test("cycles for {2} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Granitic Titan").withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Granitic Titan").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Granitic Titan") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
