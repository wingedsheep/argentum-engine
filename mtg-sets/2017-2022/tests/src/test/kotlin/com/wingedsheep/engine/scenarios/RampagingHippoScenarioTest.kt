package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rampaging Hippo (HOU) — {4}{G}{G} 5/6 trample, cycling {2}.
 */
class RampagingHippoScenarioTest : ScenarioTestBase() {
    init {
        test("has trample") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Rampaging Hippo")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val creature = game.findPermanent("Rampaging Hippo")!!
            game.state.projectedState.hasKeyword(creature, Keyword.TRAMPLE) shouldBe true
        }

        test("cycles for {2} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Rampaging Hippo").withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Rampaging Hippo").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Rampaging Hippo") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
