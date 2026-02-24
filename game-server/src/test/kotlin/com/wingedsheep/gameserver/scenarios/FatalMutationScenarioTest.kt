package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Fatal Mutation:
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * When enchanted creature is turned face up, destroy it. It can't be regenerated.
 */
class FatalMutationScenarioTest : ScenarioTestBase() {

    init {
        context("Fatal Mutation aura trigger") {

            test("destroys enchanted creature when it is turned face up") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fatal Mutation")
                    .withCardInHand(2, "Battering Craghorn")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Mountain", 10)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 casts Battering Craghorn face-down for {3}
                val craghornCardId = game.state.getHand(game.player2Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                val castResult = game.execute(CastSpell(game.player2Id, craghornCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Switch to Player 1's turn to cast Fatal Mutation
                game.state = game.state.copy(
                    activePlayerId = game.player1Id,
                    priorityPlayerId = game.player1Id
                )

                // Player 1 casts Fatal Mutation targeting the face-down creature
                val castFatalMutation = game.castSpell(1, "Fatal Mutation", faceDownId)
                withClue("Cast Fatal Mutation should succeed: ${castFatalMutation.error}") {
                    castFatalMutation.error shouldBe null
                }
                game.resolveStack()

                // Verify Fatal Mutation is on the battlefield attached to the creature
                val fatalMutationId = game.findPermanent("Fatal Mutation")
                withClue("Fatal Mutation should be on the battlefield") {
                    fatalMutationId shouldNotBe null
                }

                // Switch back to Player 2's turn to turn the creature face up
                game.state = game.state.copy(
                    activePlayerId = game.player2Id,
                    priorityPlayerId = game.player2Id
                )

                // Player 2 turns the creature face up by paying morph cost {1}{R}
                val turnFaceUpResult = game.execute(TurnFaceUp(game.player2Id, faceDownId))
                withClue("Turn face up should succeed: ${turnFaceUpResult.error}") {
                    turnFaceUpResult.error shouldBe null
                }
                game.resolveStack()

                // The creature should have been destroyed by Fatal Mutation's trigger
                withClue("Battering Craghorn should not be on the battlefield") {
                    game.findPermanent("Battering Craghorn") shouldBe null
                }

                // Fatal Mutation should also be gone (aura falls off)
                withClue("Fatal Mutation should not be on the battlefield") {
                    game.findPermanent("Fatal Mutation") shouldBe null
                }

                // Both should be in the graveyard
                val p2Graveyard = game.state.getGraveyard(game.player2Id)
                val p2GraveyardNames = p2Graveyard.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                withClue("Battering Craghorn should be in Player 2's graveyard") {
                    p2GraveyardNames.contains("Battering Craghorn") shouldBe true
                }

                val p1Graveyard = game.state.getGraveyard(game.player1Id)
                val p1GraveyardNames = p1Graveyard.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                withClue("Fatal Mutation should be in Player 1's graveyard") {
                    p1GraveyardNames.contains("Fatal Mutation") shouldBe true
                }
            }
        }
    }
}
