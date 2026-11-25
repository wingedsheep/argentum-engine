package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Attended Knight (M13 #5) — {2}{W} 2/2 Creature — Human Knight.
 *   First strike
 *   When this creature enters, create a 1/1 white Soldier creature token.
 */
class AttendedKnightScenarioTest : ScenarioTestBase() {
    init {
        test("entering creates a 1/1 white Soldier token; the Knight has first strike") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Attended Knight")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Attended Knight").error shouldBe null
            game.resolveStack()

            val knight = game.findPermanent("Attended Knight")!!
            val soldiers = game.findPermanents("Soldier Token")
            withClue("exactly one Soldier token was created") { soldiers.size shouldBe 1 }
            val projected = game.state.projectedState
            projected.getPower(soldiers.single()) shouldBe 1
            projected.getToughness(soldiers.single()) shouldBe 1
            projected.hasKeyword(knight, Keyword.FIRST_STRIKE) shouldBe true
        }
    }
}
