package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Debt to the Deathless (DGM).
 *
 * Oracle: "Each opponent loses two times X life. You gain life equal to the life lost this way."
 */
class DebtToTheDeathlessScenarioTest : ScenarioTestBase() {

    init {
        context("Debt to the Deathless") {
            test("each opponent loses two times X life and the caster gains that much") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Debt to the Deathless")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val myLife = game.getLifeTotal(1)
                val theirLife = game.getLifeTotal(2)

                game.castXSpell(1, "Debt to the Deathless", 3).error shouldBe null
                game.resolveStack()

                withClue("opponent loses 2 * X = 6") {
                    game.getLifeTotal(2) shouldBe theirLife - 6
                }
                withClue("caster gains the life lost") {
                    game.getLifeTotal(1) shouldBe myLife + 6
                }
            }
        }
    }
}
