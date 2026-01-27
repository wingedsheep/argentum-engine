package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Noxious Toad's death trigger.
 *
 * Card reference:
 * - Noxious Toad (2B): Creature - Frog, 1/1
 *   "When Noxious Toad dies, each opponent discards a card."
 * - Path of Peace (3W): "Destroy target creature. Its owner gains 4 life."
 *
 * Test scenarios:
 * 1. Opponent discards when Noxious Toad dies and has cards in hand
 * 2. Opponent with empty hand - no discard needed
 * 3. Opponent with exactly one card - automatically discards
 */
class NoxiousToadScenarioTest : ScenarioTestBase() {

    init {
        context("Noxious Toad death trigger") {
            test("opponent is prompted to discard when toad dies") {
                // Setup:
                // - Player 1 controls Noxious Toad
                // - Player 2 has Path of Peace and multiple cards in hand
                // - Player 2 casts Path of Peace to destroy the toad
                // - After resolution, Player 2 (opponent of toad's controller) should discard
                val game = scenario()
                    .withPlayers("ToadPlayer", "Opponent")
                    .withCardOnBattlefield(1, "Noxious Toad")
                    .withCardInHand(2, "Path of Peace")
                    .withCardInHand(2, "Forest")      // Card to potentially discard
                    .withCardInHand(2, "Mountain")    // Another card
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(2) - 1  // -1 for casting Path of Peace

                // Player 2 casts Path of Peace targeting Noxious Toad
                val toadId = game.findPermanent("Noxious Toad")
                    ?: error("Noxious Toad should be on battlefield")

                val castResult = game.castSpell(2, "Path of Peace", toadId)
                withClue("Path of Peace should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the stack (Path of Peace resolves, destroying Noxious Toad)
                // This should trigger the death trigger
                game.resolveStack()

                // Verify Noxious Toad is dead
                withClue("Noxious Toad should be in graveyard") {
                    game.isInGraveyard(1, "Noxious Toad") shouldBe true
                }

                withClue("Noxious Toad should not be on battlefield") {
                    game.isOnBattlefield("Noxious Toad") shouldBe false
                }

                // There should be a pending decision for Player 2 to discard
                withClue("There should be a pending decision for discard") {
                    game.hasPendingDecision() shouldBe true
                }

                // Player 2 selects a card to discard (Forest)
                val forestInHand = game.findCardsInHand(2, "Forest")
                withClue("Forest should be in Player 2's hand") {
                    forestInHand.isNotEmpty() shouldBe true
                }

                val discardResult = game.selectCards(listOf(forestInHand.first()))
                withClue("Discard selection should succeed") {
                    discardResult.error shouldBe null
                }

                // Verify results
                withClue("Forest should be in Player 2's graveyard (discarded)") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                }

                withClue("Player 2's hand should be reduced by 1 (after spell and discard)") {
                    game.handSize(2) shouldBe initialHandSize - 1
                }
            }

            test("opponent with one card automatically discards when toad dies") {
                // Setup:
                // - Player 1 controls Noxious Toad
                // - Player 2 has Path of Peace and only one other card in hand
                val game = scenario()
                    .withPlayers("ToadPlayer", "Opponent")
                    .withCardOnBattlefield(1, "Noxious Toad")
                    .withCardInHand(2, "Path of Peace")
                    .withCardInHand(2, "Forest")  // Only card to discard
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 casts Path of Peace
                val toadId = game.findPermanent("Noxious Toad")!!
                game.castSpell(2, "Path of Peace", toadId)

                // Resolve the stack
                game.resolveStack()

                // With only one card in hand, it should be automatically discarded
                // (no decision needed since there's no choice)
                withClue("Forest should be in graveyard (automatically discarded)") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                }

                withClue("Player 2's hand should be empty") {
                    game.handSize(2) shouldBe 0
                }
            }

            test("nothing happens when opponent has empty hand") {
                // Setup:
                // - Player 1 controls Noxious Toad
                // - Player 2 has only Path of Peace (empty hand after casting)
                val game = scenario()
                    .withPlayers("ToadPlayer", "Opponent")
                    .withCardOnBattlefield(1, "Noxious Toad")
                    .withCardInHand(2, "Path of Peace")
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 casts Path of Peace
                val toadId = game.findPermanent("Noxious Toad")!!
                game.castSpell(2, "Path of Peace", toadId)

                // Resolve the stack
                game.resolveStack()

                // Verify Noxious Toad died
                withClue("Noxious Toad should be in graveyard") {
                    game.isInGraveyard(1, "Noxious Toad") shouldBe true
                }

                // With empty hand, there should be no decision and no error
                // The effect should resolve successfully with no cards discarded
                withClue("There should be no pending decision (empty hand)") {
                    game.hasPendingDecision() shouldBe false
                }

                withClue("Player 2's hand should be empty") {
                    game.handSize(2) shouldBe 0
                }

                withClue("Player 2's graveyard should only have Path of Peace") {
                    game.graveyardSize(2) shouldBe 1
                    game.isInGraveyard(2, "Path of Peace") shouldBe true
                }
            }
        }
    }
}
