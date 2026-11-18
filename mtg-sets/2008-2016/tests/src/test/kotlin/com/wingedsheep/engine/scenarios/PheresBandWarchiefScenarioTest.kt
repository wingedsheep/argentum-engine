package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pheres-Band Warchief (JOU #135) — {3}{G} Creature — Centaur Warrior 3/3.
 *
 *   Vigilance, trample
 *   Other Centaur creatures you control get +1/+1 and have vigilance and trample.
 */
class PheresBandWarchiefScenarioTest : ScenarioTestBase() {
    init {
        test("other Centaurs you control get +1/+1, vigilance and trample; non-Centaurs and itself don't") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Pheres-Band Warchief")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Leaf Dancer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            val warchief = game.findPermanent("Pheres-Band Warchief")!!
            val myCourser = game.findPermanent("Centaur Courser")!!
            val theirCentaur = game.findPermanent("Leaf Dancer")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("your Centaur Courser is a 4/4 with vigilance and trample") {
                projected.getPower(myCourser) shouldBe 4
                projected.getToughness(myCourser) shouldBe 4
                projected.hasKeyword(myCourser, Keyword.VIGILANCE) shouldBe true
                projected.hasKeyword(myCourser, Keyword.TRAMPLE) shouldBe true
            }
            withClue("the opponent's Centaur is untouched") {
                projected.getPower(theirCentaur) shouldBe 2
                projected.hasKeyword(theirCentaur, Keyword.TRAMPLE) shouldBe false
            }
            withClue("a non-Centaur is untouched") {
                projected.getPower(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
            }
            withClue("\"Other\" — the Warchief doesn't pump itself") {
                projected.getPower(warchief) shouldBe 3
                projected.getToughness(warchief) shouldBe 3
            }
        }
    }
}
