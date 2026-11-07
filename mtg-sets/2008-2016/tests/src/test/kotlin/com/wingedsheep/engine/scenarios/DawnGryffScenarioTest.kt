package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Dawn Gryff (EMN #19) — {2}{W} Creature — Hippogriff, 2/2.
 *
 *   Flying
 */
class DawnGryffScenarioTest : ScenarioTestBase() {

    init {
        test("Dawn Gryff is a 2/2 flier") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dawn Gryff")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gryff = game.findPermanent("Dawn Gryff")!!
            game.state.projectedState.getPower(gryff) shouldBe 2
            game.state.projectedState.getToughness(gryff) shouldBe 2
            game.state.projectedState.hasKeyword(gryff, Keyword.FLYING) shouldBe true
        }
    }
}
