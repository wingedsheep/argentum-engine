package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Polyraptor (RIX #144) — "Enrage — Whenever this creature is dealt damage, create a token that's a
 * copy of this creature."
 */
class PolyraptorScenarioTest : ScenarioTestBase() {

    init {
        test("being dealt damage creates a token copy of it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Polyraptor")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val raptor = game.findPermanent("Polyraptor")!!
            game.castSpell(1, "Shock", raptor).error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Polyraptor").size shouldBe 2
        }
    }
}
