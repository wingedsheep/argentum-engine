package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Maze Glider (DGM #13) — {5}{U} 3/5. "Flying. Multicolored creatures you control have flying."
 *
 * Proves the multicolored group filter of the Dragon's Maze Maze cycle: only your multicolored
 * creatures gain the keyword — not your monocolored ones, and not an opponent's multicolored ones.
 */
class MazeGliderScenarioTest : ScenarioTestBase() {
    init {
        test("your multicolored creatures have flying; monocolored and opposing creatures don't") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Maze Glider")
                .withCardOnBattlefield(1, "Armored Wolf-Rider")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Armored Wolf-Rider")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val glider = game.findPermanent("Maze Glider")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val wolfRiders = game.findAllPermanents("Armored Wolf-Rider")
            val projected = game.state.projectedState

            projected.hasKeyword(glider, Keyword.FLYING) shouldBe true
            val (mine, theirs) = wolfRiders.partition { projected.getController(it) == game.player1Id }
            withClue("your green-white creature gains flying") {
                projected.hasKeyword(mine.single(), Keyword.FLYING) shouldBe true
            }
            withClue("a monocolored creature you control doesn't") {
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe false
            }
            withClue("an opponent's multicolored creature doesn't") {
                projected.hasKeyword(theirs.single(), Keyword.FLYING) shouldBe false
            }
        }
    }
}
