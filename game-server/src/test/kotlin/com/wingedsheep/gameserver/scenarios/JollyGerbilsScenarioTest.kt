package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Jolly Gerbils.
 *
 * Jolly Gerbils {1}{W}
 * Creature — Hamster Citizen
 * 2/3
 * Whenever you give a gift, draw a card.
 *
 * Tests verify the GiftGiven trigger system works with gift spells.
 */
class JollyGerbilsScenarioTest : ScenarioTestBase() {

    init {
        context("Jolly Gerbils — gift trigger") {

            test("draws a card when controller gives a gift") {
                // Jolly Gerbils on battlefield, cast a gift spell choosing the gift mode
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jolly Gerbils")
                    .withCardInHand(1, "Dawn's Truce")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast Dawn's Truce with gift mode (mode index 1)
                val castResult = game.castSpellWithMode(1, "Dawn's Truce", modeIndex = 1)
                withClue("Should cast Dawn's Truce: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // After the gift mode resolves, Jolly Gerbils trigger goes on the stack
                // Resolve the trigger
                game.resolveStack()

                // Hand: -1 (cast Dawn's Truce) + 1 (drew from gift trigger) = initialHandSize
                withClue("Should have drawn a card from Jolly Gerbils trigger") {
                    game.handSize(1) shouldBe initialHandSize
                }
            }

            test("does not draw when gift is not given (non-gift mode chosen)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jolly Gerbils")
                    .withCardInHand(1, "Dawn's Truce")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast Dawn's Truce with non-gift mode (mode index 0)
                val castResult = game.castSpellWithMode(1, "Dawn's Truce", modeIndex = 0)
                withClue("Should cast Dawn's Truce: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // No gift given → no Jolly Gerbils trigger
                // Hand: -1 (cast Dawn's Truce)
                withClue("Should NOT have drawn a card (no gift given)") {
                    game.handSize(1) shouldBe initialHandSize - 1
                }
            }
        }
    }
}
