package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Master Splicer (NPH #16) — {3}{W} 1/1 Creature — Phyrexian Human Artificer.
 *
 *   When this creature enters, create a 3/3 colorless Phyrexian Golem artifact creature token.
 *   Golems you control get +1/+1.
 */
class MasterSplicerScenarioTest : ScenarioTestBase() {

    init {
        context("Master Splicer") {
            test("makes a Golem token that it pumps to 4/4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Master Splicer")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Master Splicer").error shouldBe null
                game.resolveStack()

                val golem = game.findPermanent("Phyrexian Golem Token")!!
                game.state.projectedState.getPower(golem) shouldBe 4
                game.state.projectedState.getToughness(golem) shouldBe 4
                val splicer = game.findPermanent("Master Splicer")!!
                game.state.projectedState.getPower(splicer) shouldBe 1
            }
        }
    }
}
