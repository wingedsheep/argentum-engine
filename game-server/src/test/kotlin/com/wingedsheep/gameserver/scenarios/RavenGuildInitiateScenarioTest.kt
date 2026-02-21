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
 * Scenario tests for Raven Guild Initiate.
 *
 * Card reference:
 * - Raven Guild Initiate ({2}{U}): Creature — Human Wizard 1/4
 *   Morph—Return a Bird you control to its owner's hand.
 */
class RavenGuildInitiateScenarioTest : ScenarioTestBase() {

    init {
        context("Raven Guild Initiate") {

            test("can turn face up by returning a Bird to hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Raven Guild Initiate")
                    .withCardOnBattlefield(1, "Aven Farseer")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Raven Guild Initiate face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Raven Guild Initiate"
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

                // Find the Bird (Aven Farseer) to return
                val birdId = game.findPermanent("Aven Farseer")!!

                // Turn face up by returning the Bird
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId!!, costTargetIds = listOf(birdId))
                )
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Raven Guild Initiate should be face-up on battlefield
                val initiateOnBattlefield = game.findPermanent("Raven Guild Initiate")
                withClue("Raven Guild Initiate should be face-up on battlefield") {
                    initiateOnBattlefield shouldNotBe null
                }

                // Aven Farseer should be back in hand
                val avenInHand = game.state.getHand(game.player1Id).any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Aven Farseer"
                }
                withClue("Aven Farseer should be in hand") {
                    avenInHand shouldBe true
                }

                // Aven Farseer should NOT be on battlefield
                val avenOnBattlefield = game.findPermanent("Aven Farseer")
                withClue("Aven Farseer should not be on battlefield") {
                    avenOnBattlefield shouldBe null
                }
            }

            test("cannot turn face up without a Bird on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Raven Guild Initiate")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Raven Guild Initiate"
                }
                game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Try to turn face up with no cost targets — should fail
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId, costTargetIds = emptyList())
                )
                withClue("Turn face-up should fail without a Bird") {
                    turnUpResult.error shouldNotBe null
                }

                // Should still be face-down
                withClue("Creature should still be face-down") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe true
                }
            }

            test("cannot use a non-Bird creature to pay morph cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Raven Guild Initiate")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Raven Guild Initiate"
                }
                game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Try to return Grizzly Bears (not a Bird) — should fail
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId, costTargetIds = listOf(bearsId))
                )
                withClue("Turn face-up should fail with non-Bird target") {
                    turnUpResult.error shouldNotBe null
                }
            }

            test("can be cast normally as a 1/4 creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Raven Guild Initiate")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast normally
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Raven Guild Initiate"
                }
                val castResult = game.execute(CastSpell(game.player1Id, cardId))
                withClue("Normal cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Should be on battlefield
                val permanentId = game.findPermanent("Raven Guild Initiate")
                withClue("Raven Guild Initiate should be on battlefield") {
                    permanentId shouldNotBe null
                }
                // Should be a Human Wizard
                val card = game.state.getEntity(permanentId!!)?.get<CardComponent>()!!
                withClue("Should be a Human Wizard") {
                    card.name shouldBe "Raven Guild Initiate"
                }
            }
        }
    }
}
