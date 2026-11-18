package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Felhide Petrifier (JOU #70) — {2}{B} Creature — Minotaur Warrior 2/3.
 *
 *   Deathtouch
 *   Other Minotaur creatures you control have deathtouch.
 */
class FelhidePetrifierScenarioTest : ScenarioTestBase() {
    init {
        test("only your other Minotaurs gain deathtouch") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Felhide Petrifier")
                .withCardOnBattlefield(1, "Pensive Minotaur")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Pensive Minotaur")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val petrifier = game.findPermanent("Felhide Petrifier")!!
            val minotaurs = game.findPermanents("Pensive Minotaur")
            val mine = minotaurs.single { game.state.getBattlefield(game.player1Id).contains(it) }
            val theirs = minotaurs.single { game.state.getBattlefield(game.player2Id).contains(it) }
            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = game.state.projectedState

            withClue("the Petrifier has its own printed deathtouch") {
                projected.hasKeyword(petrifier, Keyword.DEATHTOUCH) shouldBe true
            }
            withClue("your other Minotaur has deathtouch") {
                projected.hasKeyword(mine, Keyword.DEATHTOUCH) shouldBe true
            }
            withClue("a non-Minotaur doesn't") {
                projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
            }
            withClue("an opponent's Minotaur doesn't") {
                projected.hasKeyword(theirs, Keyword.DEATHTOUCH) shouldBe false
            }
        }
    }
}
