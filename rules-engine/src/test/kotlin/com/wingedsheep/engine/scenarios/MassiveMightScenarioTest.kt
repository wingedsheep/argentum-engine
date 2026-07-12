package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Massive Might (VOW #208) — {G} Instant.
 *
 *   Target creature gets +2/+2 and gains trample until end of turn.
 *
 * Exercises the combined pump + keyword grant on a single target.
 */
class MassiveMightScenarioTest : ScenarioTestBase() {

    init {
        context("Massive Might pump + trample") {

            test("target creature gets +2/+2 and gains trample until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Massive Might")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 target
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Grizzly Bears does not start with trample") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
                }

                game.castSpell(1, "Massive Might", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears gets +2/+2 (becomes 4/4)") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
                withClue("Grizzly Bears gains trample") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
            }
        }
    }
}
