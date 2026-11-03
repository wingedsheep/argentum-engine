package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Axis of Mortality (Ixalan, {4}{W}{W} Enchantment).
 *
 *   "At the beginning of your upkeep, you may have two target players exchange life totals."
 *
 * The exchange runs between the two chosen targets, not between the controller and one target.
 */
class AxisOfMortalityScenarioTest : ScenarioTestBase() {

    init {
        fun upkeepTrigger(accept: Boolean): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Axis of Mortality")
                .withLifeTotal(1, 5)
                .withLifeTotal(2, 20)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.selectTargets(listOf(game.player1Id, game.player2Id))
            game.resolveStack()
            game.answerYesNo(accept)
            game.resolveStack()
            return game
        }

        test("the two target players exchange life totals") {
            val game = upkeepTrigger(accept = true)
            withClue("Player (5) takes the opponent's former total") { game.getLifeTotal(1) shouldBe 20 }
            withClue("Opponent (20) takes the player's former total") { game.getLifeTotal(2) shouldBe 5 }
        }

        test("declining the may leaves both totals alone") {
            val game = upkeepTrigger(accept = false)
            game.getLifeTotal(1) shouldBe 5
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
