package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Aven Fateshaper:
 * {6}{U}
 * Creature — Bird Wizard
 * 4/5
 * Flying
 * When Aven Fateshaper enters the battlefield, look at the top four cards of your
 * library, then put them back in any order.
 * {4}{U}: Look at the top four cards of your library, then put them back in any order.
 */
class AvenFateshaperScenarioTest : ScenarioTestBase() {

    init {
        context("Aven Fateshaper ETB trigger") {

            test("ETB trigger lets you reorder top 4 cards of library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Aven Fateshaper")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Aven Fateshaper
                val castResult = game.castSpell(1, "Aven Fateshaper")
                castResult.error shouldBe null

                // Resolve the spell (both players pass priority)
                game.resolveStack()

                // ETB trigger goes on the stack, resolve it
                game.resolveStack()

                // Should have a reorder decision pending
                game.hasPendingDecision() shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ReorderLibraryDecision>()
                decision as ReorderLibraryDecision
                decision.cards.size shouldBe 4

                // Get the top 4 cards and reverse them
                val libraryZone = ZoneKey(game.player1Id, Zone.LIBRARY)
                val topFour = game.state.getZone(libraryZone).take(4)
                val reversedOrder = topFour.reversed()

                // Submit the new order
                game.submitDecision(OrderedResponse(decision.id, reversedOrder))

                // Game should continue - Aven Fateshaper should be on the battlefield
                game.isOnBattlefield("Aven Fateshaper") shouldBe true

                // Verify the library was reordered
                val libraryAfter = game.state.getZone(libraryZone)
                // The reversed cards should be on top
                libraryAfter.take(4) shouldBe reversedOrder
            }
        }

        context("Aven Fateshaper activated ability") {

            test("activated ability lets you reorder top 4 cards for {4}{U}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aven Fateshaper")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fateshaperId = game.findPermanent("Aven Fateshaper")!!

                // Find the activated ability
                val cardDef = cardRegistry.getCard("Aven Fateshaper")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the ability
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = fateshaperId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )
                result.error shouldBe null

                // Resolve the ability
                game.resolveStack()

                // Should have a reorder decision pending
                game.hasPendingDecision() shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ReorderLibraryDecision>()
                decision as ReorderLibraryDecision
                decision.cards.size shouldBe 4

                // Get the top 4 cards and reverse them
                val libraryZone = ZoneKey(game.player1Id, Zone.LIBRARY)
                val topFour = game.state.getZone(libraryZone).take(4)
                val reversedOrder = topFour.reversed()

                // Submit the new order
                game.submitDecision(OrderedResponse(decision.id, reversedOrder))

                // Verify the library was reordered
                val libraryAfter = game.state.getZone(libraryZone)
                libraryAfter.take(4) shouldBe reversedOrder
            }
        }
    }
}
