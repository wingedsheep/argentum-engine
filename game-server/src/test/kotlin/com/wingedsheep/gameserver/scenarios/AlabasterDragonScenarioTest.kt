package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Alabaster Dragon's death trigger.
 *
 * This tests the engine logic directly (not through WebSocket protocol).
 * For protocol-level tests, see GameFlowTest.
 *
 * Scenario:
 * - Player 1 controls Alabaster Dragon on the battlefield
 * - Player 2 casts Hand of Death targeting Alabaster Dragon
 * - After resolution, Alabaster Dragon should be shuffled into owner's library
 *   (not in graveyard)
 *
 * Card references:
 * - Alabaster Dragon (4WW): 4/4 Flying. "When Alabaster Dragon dies, shuffle it into its owner's library."
 * - Hand of Death (2B): "Destroy target nonblack creature."
 */
class AlabasterDragonScenarioTest : ScenarioTestBase() {

    init {
        context("Alabaster Dragon death trigger") {
            test("shuffles into library when destroyed by Hand of Death") {
                // Setup the scenario:
                // - Player 1 has Alabaster Dragon on battlefield
                // - Player 2 has Hand of Death in hand with 3 Swamps (enough mana to cast)
                // - It's Player 2's main phase with priority
                val game = scenario()
                    .withPlayers("DragonPlayer", "DeathPlayer")
                    .withCardOnBattlefield(1, "Alabaster Dragon")  // Player 1's dragon
                    .withCardInHand(2, "Hand of Death")            // Player 2's removal
                    .withLandsOnBattlefield(2, "Swamp", 3)         // Enough mana for {2}{B}
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Record initial state
                val dragonId = game.findPermanent("Alabaster Dragon")
                    ?: error("Dragon should be on battlefield")
                val libraryBefore = game.librarySize(1)

                // Player 2 casts Hand of Death targeting the dragon
                val castResult = game.castSpell(2, "Hand of Death", dragonId)
                withClue("Hand of Death should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Both players pass priority to resolve
                game.resolveStack()

                // Verify results
                withClue("Alabaster Dragon should NOT be on battlefield after destruction") {
                    game.isOnBattlefield("Alabaster Dragon") shouldBe false
                }

                withClue("Alabaster Dragon should NOT be in graveyard (death trigger shuffles it)") {
                    game.isInGraveyard(1, "Alabaster Dragon") shouldBe false
                }

                withClue("Player 1's library size should increase by 1 (dragon shuffled in)") {
                    game.librarySize(1) shouldBe libraryBefore + 1
                }
            }
        }
    }
}
