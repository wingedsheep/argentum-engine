package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Thundering Ceratok (WAR #179) — {4}{G} Creature — Rhino 4/5, trample.
 *
 *   When this creature enters, other creatures you control gain trample until end of turn.
 */
class ThunderingCeratokScenarioTest : ScenarioTestBase() {

    init {
        test("entering grants trample to your other creatures only") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Thundering Ceratok")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Thundering Ceratok").error shouldBe null
            game.resolveStack()

            val self = game.findPermanent("Thundering Ceratok")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val projected = game.state.projectedState
            projected.hasKeyword(self, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
        }
    }
}
