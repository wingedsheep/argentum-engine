package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Dark Remedy (M20 #321) — {1}{B} Instant.
 *
 *   Target creature gets +1/+3 until end of turn.
 */
class DarkRemedyScenarioTest : ScenarioTestBase() {

    init {
        test("target creature gets +1/+3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Dark Remedy")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Dark Remedy", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 5
            game.isInGraveyard(1, "Dark Remedy") shouldBe true
        }
    }
}
