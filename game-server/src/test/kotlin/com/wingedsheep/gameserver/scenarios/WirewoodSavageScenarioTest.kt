package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Wirewood Savage's triggered ability.
 *
 * Card reference:
 * - Wirewood Savage (2G): 2/2 Creature - Elf
 *   "Whenever a Beast enters the battlefield, you may draw a card."
 */
class WirewoodSavageScenarioTest : ScenarioTestBase() {

    init {
        context("Wirewood Savage tribal draw trigger") {
            test("draws card when Beast enters under your control") {
                // Setup: Player 1 has Wirewood Savage on battlefield and a Beast in hand
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Wirewood Savage")
                    .withCardInHand(1, "Barkhide Mauler") // Beast creature
                    .withCardInLibrary(1, "Forest") // Card to draw
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast the Beast
                val castResult = game.castSpell(1, "Barkhide Mauler")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the creature spell
                game.resolveStack()

                // Choose to draw when the trigger resolves (it's optional)
                game.answerYesNo(true)
                game.resolveStack()

                // Should have drawn a card (minus the Beast that was played)
                // Initial hand - 1 (Beast) + 1 (draw) = initial hand
                withClue("Player should have drawn a card from Wirewood Savage trigger") {
                    game.handSize(1) shouldBe initialHandSize
                }

                // Beast should be on battlefield
                withClue("Barkhide Mauler should be on the battlefield") {
                    game.isOnBattlefield("Barkhide Mauler") shouldBe true
                }
            }

            test("can decline to draw when Beast enters") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Wirewood Savage")
                    .withCardInHand(1, "Barkhide Mauler")
                    .withCardInLibrary(1, "Forest") // Card available to draw (but won't)
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast the Beast
                game.castSpell(1, "Barkhide Mauler")
                game.resolveStack()

                // Decline to draw
                game.answerYesNo(false)
                game.resolveStack()

                // Should NOT have drawn a card
                // Initial hand - 1 (Beast) = initial hand - 1
                withClue("Player should not have drawn (declined trigger)") {
                    game.handSize(1) shouldBe initialHandSize - 1
                }
            }

            test("triggers when opponent plays a Beast") {
                val game = scenario()
                    .withPlayers("Elf Player", "Beast Player")
                    .withCardOnBattlefield(1, "Wirewood Savage")
                    .withCardInHand(2, "Barkhide Mauler")
                    .withCardInLibrary(1, "Forest") // Card for player 1 to draw
                    .withLandsOnBattlefield(2, "Forest", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Opponent casts Beast
                game.castSpell(2, "Barkhide Mauler")
                game.resolveStack()

                // Player 1 chooses to draw
                game.answerYesNo(true)
                game.resolveStack()

                // Player 1 should have drawn
                withClue("Wirewood Savage owner should draw when opponent's Beast enters") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }
            }

            test("does not trigger for non-Beast creatures") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Wirewood Savage")
                    .withCardInHand(1, "Elvish Warrior") // Elf, not a Beast
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast the Elf
                game.castSpell(1, "Elvish Warrior")
                game.resolveStack()

                // No trigger should occur - hand size should just decrease by 1
                withClue("Wirewood Savage should not trigger for Elf") {
                    game.handSize(1) shouldBe initialHandSize - 1
                }
            }
        }
    }
}
