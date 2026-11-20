package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Spike Jester (DGM #106) — {B}{R} 3/1 Creature — Goblin Warrior.
 *
 *   Haste
 */
class SpikeJesterScenarioTest : ScenarioTestBase() {
    init {
        test("is a 3/1 with haste") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Spike Jester")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val id = game.findPermanent("Spike Jester")!!
            val projected = game.state.projectedState
            projected.getPower(id) shouldBe 3
            projected.getToughness(id) shouldBe 1
            projected.hasKeyword(id, Keyword.HASTE) shouldBe true
        }
    }
}
