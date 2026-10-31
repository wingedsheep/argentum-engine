package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Loxodon Sergeant (WAR #21) — {3}{W} Creature — Elephant Soldier 3/3, vigilance.
 *
 *   When this creature enters, other creatures you control gain vigilance until end of turn.
 */
class LoxodonSergeantScenarioTest : ScenarioTestBase() {

    init {
        test("entering grants vigilance to your other creatures only") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Loxodon Sergeant")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Loxodon Sergeant").error shouldBe null
            game.resolveStack()

            val self = game.findPermanent("Loxodon Sergeant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val projected = game.state.projectedState
            projected.hasKeyword(self, Keyword.VIGILANCE) shouldBe true
            projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            projected.hasKeyword(giant, Keyword.VIGILANCE) shouldBe false
        }
    }
}
