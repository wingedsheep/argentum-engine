package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Moaning Wall (HOU) — {2}{B} 0/5 defender, cycling {2}.
 */
class MoaningWallScenarioTest : ScenarioTestBase() {
    init {
        test("has defender") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Moaning Wall")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val creature = game.findPermanent("Moaning Wall")!!
            game.state.projectedState.hasKeyword(creature, Keyword.DEFENDER) shouldBe true
        }

        test("cycles for {2} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Moaning Wall").withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Moaning Wall").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Moaning Wall") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
