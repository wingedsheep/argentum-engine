package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Steeple Roc (DGM #8) — {4}{W} 3/1 Creature — Bird.
 *
 *   Flying, first strike
 */
class SteepleRocScenarioTest : ScenarioTestBase() {
    init {
        test("is a 3/1 with flying and first strike") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Steeple Roc")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val id = game.findPermanent("Steeple Roc")!!
            val projected = game.state.projectedState
            projected.getPower(id) shouldBe 3
            projected.getToughness(id) shouldBe 1
            projected.hasKeyword(id, Keyword.FLYING) shouldBe true
            projected.hasKeyword(id, Keyword.FIRST_STRIKE) shouldBe true
        }
    }
}
