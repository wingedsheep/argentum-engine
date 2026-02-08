package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.identity.CardComponent
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FleetingAvenScenarioTest : ScenarioTestBase() {

    init {
        context("Fleeting Aven") {
            test("cycling a card returns Fleeting Aven to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Fleeting Aven")
                    .withCardInHand(1, "Barren Moor")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify Fleeting Aven is on the battlefield
                withClue("Fleeting Aven should be on the battlefield") {
                    game.findPermanent("Fleeting Aven") shouldNotBe null
                }

                // Cycle Barren Moor - triggers Fleeting Aven's return-to-hand
                game.cycleCard(1, "Barren Moor")

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // Fleeting Aven should no longer be on the battlefield
                withClue("Fleeting Aven should have left the battlefield") {
                    game.findPermanent("Fleeting Aven") shouldBe null
                }

                // Fleeting Aven should be in the owner's hand
                val hand = game.state.getHand(game.player1Id)
                val fleetingAvenInHand = hand.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Fleeting Aven"
                }
                withClue("Fleeting Aven should be in the owner's hand") {
                    fleetingAvenInHand shouldBe true
                }
            }

            test("opponent cycling a card also returns Fleeting Aven to owner's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Fleeting Aven")
                    .withCardInHand(2, "Forgotten Cave")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify Fleeting Aven is on the battlefield
                withClue("Fleeting Aven should be on the battlefield") {
                    game.findPermanent("Fleeting Aven") shouldNotBe null
                }

                // Opponent cycles Forgotten Cave
                game.cycleCard(2, "Forgotten Cave")

                // Resolve the triggered ability
                game.resolveStack()

                // Fleeting Aven should have returned to Player1's hand
                withClue("Fleeting Aven should have left the battlefield") {
                    game.findPermanent("Fleeting Aven") shouldBe null
                }

                val hand = game.state.getHand(game.player1Id)
                val fleetingAvenInHand = hand.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Fleeting Aven"
                }
                withClue("Fleeting Aven should be in Player1's hand") {
                    fleetingAvenInHand shouldBe true
                }
            }

            test("no cycling means Fleeting Aven stays on battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Fleeting Aven")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Pass priority without cycling anything
                game.passPriority()
                game.passPriority()

                // Fleeting Aven should still be on the battlefield
                withClue("Fleeting Aven should remain on the battlefield when no cycling occurs") {
                    game.findPermanent("Fleeting Aven") shouldNotBe null
                }
            }
        }
    }
}
