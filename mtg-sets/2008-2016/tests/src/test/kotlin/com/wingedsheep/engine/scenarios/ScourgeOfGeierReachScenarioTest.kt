package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scourge of Geier Reach (ISD #162) — {3}{R}{R} 3/3 Elemental.
 *
 *   This creature gets +1/+1 for each creature your opponents control.
 */
class ScourgeOfGeierReachScenarioTest : ScenarioTestBase() {

    init {
        test("gets +1/+1 per opposing creature, ignoring your own, and shrinks when one dies") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Scourge of Geier Reach")
                .withCardOnBattlefield(1, "Elite Vanguard")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scourge = game.findPermanent("Scourge of Geier Reach")!!
            game.state.projectedState.getPower(scourge) shouldBe 6
            game.state.projectedState.getToughness(scourge) shouldBe 6

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(scourge) shouldBe 5
            game.state.projectedState.getToughness(scourge) shouldBe 5
        }
    }
}
