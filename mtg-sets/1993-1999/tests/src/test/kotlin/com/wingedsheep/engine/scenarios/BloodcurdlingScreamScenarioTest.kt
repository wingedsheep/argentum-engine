package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bloodcurdling Scream (P02) — "Target creature gets +X/+0 until end of turn."
 */
class BloodcurdlingScreamScenarioTest : ScenarioTestBase() {

    init {
        test("gives the target +X/+0 until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Bloodcurdling Scream")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = game.castXSpell(1, "Bloodcurdling Scream", xValue = 3, targetId = bears)
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getToughness(bears) shouldBe 2
        }
    }
}
