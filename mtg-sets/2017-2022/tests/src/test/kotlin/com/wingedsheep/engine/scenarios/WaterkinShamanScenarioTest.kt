package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Waterkin Shaman (M20 #288) — {1}{U} 2/1 Creature — Elemental Shaman.
 *
 *   Whenever a creature you control with flying enters, this creature gets +1/+1 until end of turn.
 */
class WaterkinShamanScenarioTest : ScenarioTestBase() {

    init {
        context("Waterkin Shaman") {
            test("a flier entering under your control pumps it; a non-flier doesn't") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Waterkin Shaman")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Wind Drake")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shaman = game.findPermanent("Waterkin Shaman")!!
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(shaman) shouldBe 2

                game.castSpell(1, "Wind Drake").error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(shaman) shouldBe 3
                game.state.projectedState.getToughness(shaman) shouldBe 2
            }
        }
    }
}
