package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Putrid Raptor.
 *
 * Card reference:
 * - Putrid Raptor ({4}{B}{B}): Creature — Zombie Dinosaur Beast 4/4
 *   Morph—Discard a Zombie card.
 */
class PutridRaptorScenarioTest : ScenarioTestBase() {

    init {
        context("Putrid Raptor") {

            test("can turn face up by discarding a Zombie card from hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Putrid Raptor")
                    .withCardInHand(1, "Zombie Cutthroat") // Zombie card to discard
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Putrid Raptor face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Putrid Raptor"
                }
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature on battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Find the Zombie Cutthroat in hand to discard
                val zombieInHand = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Zombie Cutthroat"
                }

                // Turn face up by discarding the Zombie card
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId!!, costTargetIds = listOf(zombieInHand))
                )
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Putrid Raptor should be face-up on battlefield
                val raptorOnBattlefield = game.findPermanent("Putrid Raptor")
                withClue("Putrid Raptor should be face-up on battlefield") {
                    raptorOnBattlefield shouldNotBe null
                }

                // Zombie Cutthroat should be in graveyard (discarded)
                val zombiesInGraveyard = game.state.getZone(
                    com.wingedsheep.engine.state.ZoneKey(game.player1Id, Zone.GRAVEYARD)
                ).count { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Zombie Cutthroat"
                }
                withClue("Zombie Cutthroat should be in graveyard") {
                    zombiesInGraveyard shouldBe 1
                }
            }

            test("cannot turn face up without a Zombie card in hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Putrid Raptor")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Putrid Raptor face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Putrid Raptor"
                }
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature on battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Hand should be empty (no Zombie to discard)
                val handSize = game.state.getHand(game.player1Id).size
                withClue("Hand should be empty") {
                    handSize shouldBe 0
                }

                // Try to turn face up with empty costTargetIds — should fail
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId!!, costTargetIds = emptyList())
                )
                withClue("Turn face-up should fail without a Zombie card to discard") {
                    turnUpResult.error shouldNotBe null
                }
            }

            test("cannot discard a non-Zombie card to pay morph cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Putrid Raptor")
                    .withCardInHand(1, "Glory Seeker") // Not a Zombie
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Putrid Raptor face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Putrid Raptor"
                }
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature on battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Find the Glory Seeker in hand (non-Zombie)
                val glorySeekerInHand = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Glory Seeker"
                }

                // Try to discard non-Zombie — should fail validation
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId!!, costTargetIds = listOf(glorySeekerInHand))
                )
                withClue("Turn face-up should fail with non-Zombie discard") {
                    turnUpResult.error shouldNotBe null
                }
            }
        }
    }
}
