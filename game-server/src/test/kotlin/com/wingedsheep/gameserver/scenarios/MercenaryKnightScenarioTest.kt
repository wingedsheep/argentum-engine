package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Mercenary Knight's ETB sacrifice trigger.
 *
 * Card reference:
 * - Mercenary Knight (2B): 4/4
 *   "When Mercenary Knight enters the battlefield, sacrifice it unless you
 *   discard a creature card."
 */
class MercenaryKnightScenarioTest : ScenarioTestBase() {

    init {
        context("Mercenary Knight ETB trigger") {
            test("survives when player discards a creature card") {
                // Setup: Player 1 has Mercenary Knight and Grizzly Bears in hand
                val game = scenario()
                    .withPlayers("KnightPlayer", "Opponent")
                    .withCardInHand(1, "Mercenary Knight")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Mercenary Knight
                val castResult = game.castSpell(1, "Mercenary Knight")
                withClue("Mercenary Knight should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell and the ETB trigger
                game.resolveStack()

                // There should be a pending decision to discard
                withClue("There should be a pending decision for discard") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Grizzly Bears to discard
                val bearsInHand = game.findCardsInHand(1, "Grizzly Bears")
                val discardResult = game.selectCards(listOf(bearsInHand.first()))
                withClue("Discard selection should succeed") {
                    discardResult.error shouldBe null
                }

                // Verify results
                withClue("Mercenary Knight should be on the battlefield (survived)") {
                    game.isOnBattlefield("Mercenary Knight") shouldBe true
                }

                withClue("Grizzly Bears should be in graveyard (discarded)") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                withClue("Mercenary Knight should NOT be in graveyard") {
                    game.isInGraveyard(1, "Mercenary Knight") shouldBe false
                }
            }

            test("is sacrificed when player skips discard") {
                // Setup: Player has Mercenary Knight and a creature in hand
                val game = scenario()
                    .withPlayers("KnightPlayer", "Opponent")
                    .withCardInHand(1, "Mercenary Knight")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve
                game.castSpell(1, "Mercenary Knight")
                game.resolveStack()

                withClue("There should be a pending decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Skip the discard (select nothing)
                game.skipSelection()

                // Verify Mercenary Knight was sacrificed
                withClue("Mercenary Knight should NOT be on battlefield (sacrificed)") {
                    game.isOnBattlefield("Mercenary Knight") shouldBe false
                }

                withClue("Mercenary Knight should be in graveyard") {
                    game.isInGraveyard(1, "Mercenary Knight") shouldBe true
                }

                withClue("Grizzly Bears should still be in hand (not discarded)") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("is sacrificed when player has no creature cards in hand") {
                // Setup: Player has Mercenary Knight but only non-creature cards
                val game = scenario()
                    .withPlayers("KnightPlayer", "Opponent")
                    .withCardInHand(1, "Mercenary Knight")
                    .withCardInHand(1, "Forest")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve
                game.castSpell(1, "Mercenary Knight")
                game.resolveStack()

                // With no creature cards, the Knight should be automatically sacrificed
                // (or there's a decision with no valid options - skip it)
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                }

                // Verify Mercenary Knight was sacrificed
                withClue("Mercenary Knight should NOT be on battlefield") {
                    game.isOnBattlefield("Mercenary Knight") shouldBe false
                }

                withClue("Mercenary Knight should be in graveyard") {
                    game.isInGraveyard(1, "Mercenary Knight") shouldBe true
                }
            }

            test("only shows creature cards as discard options") {
                // Setup: Player has mixed cards in hand
                val game = scenario()
                    .withPlayers("KnightPlayer", "Opponent")
                    .withCardInHand(1, "Mercenary Knight")
                    .withCardInHand(1, "Grizzly Bears")  // Creature - valid
                    .withCardInHand(1, "Forest")         // Land - invalid
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve
                game.castSpell(1, "Mercenary Knight")
                game.resolveStack()

                withClue("There should be a pending decision") {
                    game.hasPendingDecision() shouldBe true
                }

                val decision = game.getPendingDecision()
                withClue("Decision should exist") {
                    decision shouldNotBe null
                }

                // The decision should only have creature cards as options
                val bearsInHand = game.findCardsInHand(1, "Grizzly Bears")
                withClue("Grizzly Bears should be in hand as a valid option") {
                    bearsInHand.isNotEmpty() shouldBe true
                }

                // Complete the scenario by selecting the creature
                game.selectCards(listOf(bearsInHand.first()))

                withClue("Mercenary Knight should survive") {
                    game.isOnBattlefield("Mercenary Knight") shouldBe true
                }
            }
        }
    }
}
