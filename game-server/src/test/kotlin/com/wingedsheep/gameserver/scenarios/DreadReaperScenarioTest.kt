package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Dread Reaper's ETB trigger.
 *
 * Scenario:
 * - Player 1 has Dread Reaper in hand with enough mana to cast it
 * - Player 1 casts Dread Reaper
 * - After resolution, Dread Reaper should be on the battlefield
 * - Player 1 should lose 5 life (from 20 to 15)
 *
 * Card reference:
 * - Dread Reaper (3BBB): 6/5 Flying. "When Dread Reaper enters the battlefield, you lose 5 life."
 */
class DreadReaperScenarioTest : ScenarioTestBase() {

    init {
        context("Dread Reaper ETB trigger") {
            test("controller loses 5 life when Dread Reaper enters the battlefield") {
                val game = scenario()
                    .withPlayers("ReaperPlayer", "Opponent")
                    .withCardInHand(1, "Dread Reaper")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)
                lifeBefore shouldBe 20

                val castResult = game.castSpell(1, "Dread Reaper")
                withClue("Dread Reaper should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the creature spell and the ETB trigger
                game.resolveStack()

                withClue("Dread Reaper should be on the battlefield") {
                    game.isOnBattlefield("Dread Reaper") shouldBe true
                }

                withClue("Controller (Player 1) should lose 5 life") {
                    game.getLifeTotal(1) shouldBe 15
                }

                withClue("Opponent (Player 2) should not lose life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
