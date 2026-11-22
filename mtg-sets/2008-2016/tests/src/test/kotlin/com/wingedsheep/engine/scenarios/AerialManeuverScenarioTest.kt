package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Aerial Maneuver (GTC) — target creature gets +1/+1 and gains flying and first strike until end of turn. */
class AerialManeuverScenarioTest : ScenarioTestBase() {
    init {
        test("pumps the creature and grants flying and first strike") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Aerial Maneuver")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Aerial Maneuver", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
        }
    }
}
