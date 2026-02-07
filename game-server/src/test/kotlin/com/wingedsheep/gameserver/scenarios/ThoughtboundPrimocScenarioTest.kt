package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Thoughtbound Primoc.
 *
 * Card reference:
 * - Thoughtbound Primoc ({2}{U}): Creature — Bird Beast, 2/3
 *   Flying
 *   "At the beginning of your upkeep, if you control a Wizard,
 *   draw a card, then discard a card."
 *
 * Cards used:
 * - Crafty Pathmage (Creature — Human Wizard) — a Wizard to satisfy the condition
 * - Grizzly Bears (Creature — Bear) — non-Wizard creature
 */
class ThoughtboundPrimocScenarioTest : ScenarioTestBase() {

    init {
        context("Thoughtbound Primoc upkeep trigger") {

            test("loots on upkeep when you control a Wizard") {
                // Start at UNTAP step with cards in hand so discard prompts a choice
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Thoughtbound Primoc")
                    .withCardOnBattlefield(1, "Crafty Pathmage")   // Wizard
                    .withCardInHand(1, "Mountain")                  // Already in hand
                    .withCardInLibrary(1, "Forest")                 // Card to draw from loot
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val initialHandSize = game.handSize(1)

                // Advance to upkeep (untap → upkeep)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Upkeep trigger should fire. Pass priority to let it resolve.
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have pending discard decision from loot") {
                    game.hasPendingDecision() shouldBe true
                }

                // After loot's draw, player has Mountain + Forest = 2 cards
                // Must choose 1 to discard
                val handCards = game.state.getHand(game.player1Id)
                game.selectCards(listOf(handCards.first()))

                // After loot: drew 1, discarded 1 = net 0 change from initial
                withClue("Hand size should be same as initial (drew 1, discarded 1)") {
                    game.handSize(1) shouldBe initialHandSize
                }

                withClue("Graveyard should have the discarded card") {
                    game.graveyardSize(1) shouldBe 1
                }
            }

            test("does NOT loot on upkeep without a Wizard") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Thoughtbound Primoc")
                    .withCardOnBattlefield(1, "Grizzly Bears")     // Not a Wizard
                    .withCardInHand(1, "Mountain")                  // Card in hand
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance to upkeep
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                val handSizeAtUpkeep = game.handSize(1)
                val graveyardSizeAtUpkeep = game.graveyardSize(1)

                // Pass priority through upkeep — ConditionalEffect condition should fail
                game.passPriority()
                game.passPriority()

                withClue("Hand size should not have changed during upkeep (no loot without Wizard)") {
                    game.handSize(1) shouldBe handSizeAtUpkeep
                }

                withClue("Graveyard should not have changed during upkeep (no discard without Wizard)") {
                    game.graveyardSize(1) shouldBe graveyardSizeAtUpkeep
                }
            }
        }
    }
}
