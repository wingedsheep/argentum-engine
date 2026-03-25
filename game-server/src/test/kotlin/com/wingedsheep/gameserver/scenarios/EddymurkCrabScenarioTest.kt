package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Eddymurk Crab.
 *
 * Card reference:
 * - Eddymurk Crab ({5}{U}{U}): Creature — Elemental Crab (5/5)
 *   Flash
 *   This spell costs {1} less to cast for each instant and sorcery card in your graveyard.
 *   This creature enters tapped if it's not your turn.
 *   When this creature enters, tap up to two target creatures.
 */
class EddymurkCrabScenarioTest : ScenarioTestBase() {

    init {
        context("Eddymurk Crab cost reduction") {
            test("costs {1} less per instant/sorcery in graveyard") {
                // Eddymurk Crab costs {5}{U}{U} = 7 total (5 generic + 2 blue)
                // With 3 instants in graveyard, costs {2}{U}{U} = 4 total
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eddymurk Crab")
                    .withCardInGraveyard(1, "Early Winter")    // instant
                    .withCardInGraveyard(1, "Dawn's Truce")    // instant
                    .withCardInGraveyard(1, "Nocturnal Hunger") // instant
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // With 3 instants in graveyard, cost should be {2}{U}{U} = 4 mana
                val castResult = game.castSpell(1, "Eddymurk Crab")
                castResult.error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Eddymurk Crab") shouldBe true
            }

            test("creature cards in graveyard do not reduce cost") {
                // Only 1 instant in graveyard, so cost is {4}{U}{U} = 6 total
                // With only 5 Islands, we can't cast it
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eddymurk Crab")
                    .withCardInGraveyard(1, "Early Winter")    // instant - counts
                    .withCardInGraveyard(1, "Glory Seeker")    // creature - doesn't count
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // With 1 instant in graveyard, cost is {4}{U}{U} = 6 mana
                // We only have 5 Islands (5 mana), can't cast
                val castResult = game.castSpell(1, "Eddymurk Crab")
                castResult.error shouldNotBe null
            }
        }

        context("Eddymurk Crab enters tapped") {
            test("enters tapped on opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eddymurk Crab")
                    .withCardInGraveyard(1, "Early Winter")
                    .withCardInGraveyard(1, "Dawn's Truce")
                    .withCardInGraveyard(1, "Nocturnal Hunger")
                    .withCardInGraveyard(1, "Crumb and Get It")
                    .withCardInGraveyard(1, "Playful Shove") // sorcery
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)  // It's player 2's turn
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // 5 instants/sorceries in graveyard, cost = {U}{U} (2 mana)
                val castResult = game.castSpell(1, "Eddymurk Crab")
                castResult.error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Eddymurk Crab") shouldBe true
                val crabId = game.findPermanent("Eddymurk Crab")!!
                game.state.getEntity(crabId)?.has<TappedComponent>() shouldBe true
            }

            test("enters untapped on own turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eddymurk Crab")
                    .withCardInGraveyard(1, "Early Winter")
                    .withCardInGraveyard(1, "Dawn's Truce")
                    .withCardInGraveyard(1, "Nocturnal Hunger")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)  // It's player 1's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Eddymurk Crab")
                castResult.error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Eddymurk Crab") shouldBe true
                val crabId = game.findPermanent("Eddymurk Crab")!!
                game.state.getEntity(crabId)?.has<TappedComponent>() shouldBe false
            }
        }

        context("Eddymurk Crab ETB trigger") {
            test("taps up to two target creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eddymurk Crab")
                    .withCardInGraveyard(1, "Early Winter")
                    .withCardInGraveyard(1, "Dawn's Truce")
                    .withCardInGraveyard(1, "Nocturnal Hunger")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardOnBattlefield(2, "Goblin Sledder")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Eddymurk Crab")
                castResult.error shouldBe null
                game.resolveStack()

                // ETB trigger goes on stack - select two targets
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val goblinSledderID = game.findPermanent("Goblin Sledder")!!
                game.selectTargets(listOf(glorySeekerID, goblinSledderID))
                game.resolveStack()

                game.state.getEntity(glorySeekerID)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(goblinSledderID)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
