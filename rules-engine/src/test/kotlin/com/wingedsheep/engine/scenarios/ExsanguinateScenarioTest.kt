package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Exsanguinate (SOM).
 *
 * Oracle: "Each opponent loses X life. You gain life equal to the life lost this way."
 *
 * Exercises the new [com.wingedsheep.sdk.dsl.Effects.DrainLife] primitive: the gain equals
 * the life *actually* lost, as a single life-gain event after all losses.
 */
class ExsanguinateScenarioTest : ScenarioTestBase() {

    private fun game(x: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Exsanguinate")
        .withLandsOnBattlefield(1, "Swamp", 2 + x)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Exsanguinate") {
            test("each opponent loses X life and the caster gains that much") {
                val game = game(3)
                val myLife = game.getLifeTotal(1)
                val theirLife = game.getLifeTotal(2)

                game.castXSpell(1, "Exsanguinate", 3).error shouldBe null
                game.resolveStack()

                withClue("opponent loses X = 3") {
                    game.getLifeTotal(2) shouldBe theirLife - 3
                }
                withClue("caster gains the life lost") {
                    game.getLifeTotal(1) shouldBe myLife + 3
                }
            }

            test("X = 0 changes no life totals") {
                val game = game(0)
                val myLife = game.getLifeTotal(1)
                val theirLife = game.getLifeTotal(2)

                game.castXSpell(1, "Exsanguinate", 0).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe myLife
                game.getLifeTotal(2) shouldBe theirLife
            }

            test("the full X is lost (and gained) even when it takes the opponent below 0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Exsanguinate")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withLifeTotal(2, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val myLife = game.getLifeTotal(1)

                game.castXSpell(1, "Exsanguinate", 5).error shouldBe null
                game.resolveStack()

                withClue("life loss is not capped at the opponent's remaining life (CR 119.6)") {
                    game.getLifeTotal(1) shouldBe myLife + 5
                }
            }
        }
    }
}
