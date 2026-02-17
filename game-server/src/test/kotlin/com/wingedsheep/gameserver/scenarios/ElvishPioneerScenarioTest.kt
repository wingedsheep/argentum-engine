package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Elvish Pioneer.
 *
 * Card reference:
 * - Elvish Pioneer (G): 1/1 Creature — Elf Druid
 *   When Elvish Pioneer enters the battlefield, you may put a basic land card
 *   from your hand onto the battlefield tapped.
 */
class ElvishPioneerScenarioTest : ScenarioTestBase() {

    init {
        context("Elvish Pioneer ETB trigger") {
            test("putting a basic land from hand onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Elvish Pioneer")
                    .withCardInHand(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast Elvish Pioneer
                val castResult = game.castSpell(1, "Elvish Pioneer")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve creature spell — ETB trigger fires, shows land selection directly
                game.resolveStack()

                // Select the Forest from hand
                val forestInHand = game.findCardsInHand(1, "Forest")
                withClue("Should have a Forest in hand to select") {
                    forestInHand.size shouldBe 1
                }
                game.selectCards(forestInHand)

                // Resolve the triggered ability
                game.resolveStack()

                // Forest should be on the battlefield
                val forests = game.findAllPermanents("Forest")
                withClue("Should have 2 Forests on battlefield (1 original + 1 from Pioneer)") {
                    forests.size shouldBe 2
                }

                // The newly placed Forest should be tapped
                val newForest = forests.find { forestId ->
                    game.state.getEntity(forestId)?.has<TappedComponent>() == true
                }
                withClue("New Forest should be tapped") {
                    newForest shouldNotBe null
                }

                // Hand should have lost the Forest
                withClue("Hand should have one fewer card") {
                    game.handSize(1) shouldBe initialHandSize - 2 // -1 for Pioneer, -1 for Forest
                }
            }

            test("declining the optional ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Elvish Pioneer")
                    .withCardInHand(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Elvish Pioneer
                game.castSpell(1, "Elvish Pioneer")
                game.resolveStack()

                // Card selection appears — skip (select nothing) to decline
                game.skipSelection()

                // Forest should stay in hand
                withClue("Forest should still be in hand") {
                    game.findCardsInHand(1, "Forest").size shouldBe 1
                }

                // Only the original Forest on battlefield
                withClue("Should have only 1 Forest on battlefield") {
                    game.findAllPermanents("Forest").size shouldBe 1
                }
            }

            test("no basic lands in hand — ability resolves with no effect") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Elvish Pioneer")
                    // No lands in hand
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Elvish Pioneer
                game.castSpell(1, "Elvish Pioneer")
                // Trigger fires but Gather finds no matching lands — resolves immediately with no effect
                game.resolveStack()

                // No additional lands on battlefield
                withClue("Should have only 1 Forest on battlefield") {
                    game.findAllPermanents("Forest").size shouldBe 1
                }
            }
        }
    }
}
