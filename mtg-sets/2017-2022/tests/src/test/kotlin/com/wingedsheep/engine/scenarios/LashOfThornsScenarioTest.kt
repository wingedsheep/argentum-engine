package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Lash of Thorns (ELD #92) — Target creature gets +2/+1 and gains deathtouch until end of turn.
 */
class LashOfThornsScenarioTest : ScenarioTestBase() {

    init {
        context("Lash of Thorns") {

            test("target creature gets +2/+1 and deathtouch") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lash of Thorns")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lash of Thorns", bears).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 3
                game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
            }
        }
    }
}
