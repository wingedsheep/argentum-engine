package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Squelching Leeches (JOU #84) — {2}{B}{B} Creature — Leech * / *.
 *
 *   Squelching Leeches's power and toughness are each equal to the number of Swamps you control.
 */
class SquelchingLeechesScenarioTest : ScenarioTestBase() {
    init {
        test("its power and toughness count only the Swamps you control") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Squelching Leeches")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(2, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val leeches = game.findPermanent("Squelching Leeches")!!
            withClue("3 Swamps you control make it a 3/3") {
                game.state.projectedState.getPower(leeches) shouldBe 3
                game.state.projectedState.getToughness(leeches) shouldBe 3
            }
        }
    }
}
