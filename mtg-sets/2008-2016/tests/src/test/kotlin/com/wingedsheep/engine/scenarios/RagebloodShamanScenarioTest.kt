package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rageblood Shaman (THS #138) — {1}{R}{R} Creature — Minotaur Shaman 2/3.
 *
 *   Trample
 *   Other Minotaur creatures you control get +1/+1 and have trample.
 */
class RagebloodShamanScenarioTest : ScenarioTestBase() {
    init {
        test("other Minotaurs you control get +1/+1 and trample; the opponent's and non-Minotaurs don't") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Rageblood Shaman")
                .withCardOnBattlefield(1, "Felhide Minotaur")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Felhide Minotaur")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            val shaman = game.findPermanent("Rageblood Shaman")!!
            val minotaurs = game.findAllPermanents("Felhide Minotaur")
            val bears = game.findPermanent("Grizzly Bears")!!
            val mine = minotaurs.single { projected.getController(it) == game.player1Id }
            val theirs = minotaurs.single { projected.getController(it) == game.player2Id }

            withClue("your Felhide Minotaur is a 3/4 trampler") {
                projected.getPower(mine) shouldBe 3
                projected.getToughness(mine) shouldBe 4
                projected.hasKeyword(mine, Keyword.TRAMPLE) shouldBe true
            }
            withClue("the opponent's Minotaur is untouched") {
                projected.getPower(theirs) shouldBe 2
                projected.hasKeyword(theirs, Keyword.TRAMPLE) shouldBe false
            }
            withClue("a non-Minotaur is untouched") {
                projected.getPower(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
            }
            withClue("the Shaman has printed trample but doesn't pump itself") {
                projected.getPower(shaman) shouldBe 2
                projected.getToughness(shaman) shouldBe 3
                projected.hasKeyword(shaman, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
