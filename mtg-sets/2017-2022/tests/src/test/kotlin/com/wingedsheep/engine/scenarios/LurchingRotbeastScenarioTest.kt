package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Lurching Rotbeast (HOU) — {3}{B} 4/2, cycling {B}.
 */
class LurchingRotbeastScenarioTest : ScenarioTestBase() {
    init {
        test("cycles for {B} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Lurching Rotbeast").withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Lurching Rotbeast").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Lurching Rotbeast") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
