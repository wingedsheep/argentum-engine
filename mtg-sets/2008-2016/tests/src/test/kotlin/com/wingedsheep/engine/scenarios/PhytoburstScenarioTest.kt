package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Phytoburst (DGM #46) — {1}{G} Sorcery.
 *
 *   Target creature gets +5/+5 until end of turn.
 */
class PhytoburstScenarioTest : ScenarioTestBase() {
    init {
        test("target creature gets +5/+5") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Phytoburst")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Phytoburst", bears).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears is a 7/7 until end of turn") {
                game.state.projectedState.getPower(bears) shouldBe 7
                game.state.projectedState.getToughness(bears) shouldBe 7
            }
        }
    }
}
