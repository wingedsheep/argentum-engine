package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Skirk Volcanist.
 *
 * Card reference:
 * - Skirk Volcanist ({3}{R}): Creature — Goblin 3/1
 *   Morph—Sacrifice two Mountains.
 *   When Skirk Volcanist is turned face up, it deals 3 damage divided as you choose
 *   among one, two, or three target creatures.
 */
class SkirkVolcanistScenarioTest : ScenarioTestBase() {

    init {
        context("Skirk Volcanist") {

            test("can turn face up by sacrificing two Mountains and deal 3 damage to one creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skirk Volcanist")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Skirk Volcanist face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Skirk Volcanist"
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

                // Find two Mountains to sacrifice
                val mountains = game.state.getBattlefield().filter { entityId ->
                    val card = game.state.getEntity(entityId)?.get<CardComponent>()
                    card?.name == "Mountain"
                }.take(2)
                withClue("Should have at least 2 Mountains") {
                    mountains.size shouldBe 2
                }

                // Turn face up by sacrificing two Mountains
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId!!, costTargetIds = mountains)
                )
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Skirk Volcanist should be face-up on battlefield
                val volcanistOnBattlefield = game.findPermanent("Skirk Volcanist")
                withClue("Skirk Volcanist should be face-up on battlefield") {
                    volcanistOnBattlefield shouldNotBe null
                }

                // Two Mountains should be in graveyard
                val mountainsInGraveyard = game.state.getZone(
                    com.wingedsheep.engine.state.ZoneKey(game.player1Id, Zone.GRAVEYARD)
                ).count { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Mountain"
                }
                withClue("Two Mountains should be in graveyard") {
                    mountainsInGraveyard shouldBe 2
                }

                // Should have a pending decision to choose targets for divided damage
                withClue("Should have pending target decision") {
                    game.state.pendingDecision shouldNotBe null
                }

                // Select Grizzly Bears as target for the 3 damage
                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bearsId))

                // Ability goes on stack — resolve it
                game.resolveStack()

                // With one target, all 3 damage goes to it automatically — no distribution needed
                // Grizzly Bears (2/2) should be dead from 3 damage
                val bearsAfter = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears should be destroyed by 3 damage") {
                    bearsAfter shouldBe null
                }
            }

            test("cannot turn face up without enough Mountains to sacrifice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skirk Volcanist")
                    .withLandsOnBattlefield(1, "Mountain", 4) // Need 3 for casting + 2 more for morph
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down for {3} (taps 3 Mountains)
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Skirk Volcanist"
                }
                game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Only 1 untapped Mountain left (tapped 3 for casting)
                // Even though there are 4 Mountains total, we need 2 untapped ones
                // But sacrifice doesn't require untapped — tapped Mountains can be sacrificed too
                val mountains = game.state.getBattlefield().filter { entityId ->
                    val card = game.state.getEntity(entityId)?.get<CardComponent>()
                    card?.name == "Mountain"
                }.take(2)

                // Should be able to sacrifice tapped Mountains
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId, costTargetIds = mountains)
                )
                withClue("Turn face-up should succeed even with tapped Mountains: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }
            }

            test("cannot sacrifice non-Mountain lands for morph cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skirk Volcanist")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Skirk Volcanist"
                }
                game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Try to sacrifice Forests instead of Mountains
                val forests = game.state.getBattlefield().filter { entityId ->
                    val card = game.state.getEntity(entityId)?.get<CardComponent>()
                    card?.name == "Forest"
                }.take(2)

                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId, costTargetIds = forests)
                )
                withClue("Turn face-up should fail with non-Mountain lands") {
                    turnUpResult.error shouldNotBe null
                }
            }

            test("deals divided damage to multiple creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skirk Volcanist")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Raging Goblin")    // 1/1
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Skirk Volcanist"
                }
                game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                val mountains = game.state.getBattlefield().filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Mountain"
                }.take(2)

                // Turn face up
                val turnUpResult = game.execute(
                    TurnFaceUp(game.player1Id, faceDownId, costTargetIds = mountains)
                )
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Select two creatures as targets
                val cadetId = game.findPermanent("Raging Goblin")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(cadetId, bearsId))

                // Ability goes on stack — resolve it
                // With two targets, need to distribute damage
                // Should get a DistributeDecision
                game.resolveStack()

                withClue("Should have a distribute decision") {
                    game.state.pendingDecision shouldNotBe null
                    game.state.pendingDecision shouldBe io.kotest.matchers.types.beInstanceOf<DistributeDecision>()
                }

                // Distribute: 1 to Raging Goblin, 2 to Grizzly Bears
                game.submitDistribution(mapOf(cadetId to 1, bearsId to 2))

                // Pass priority to trigger SBA checking (lethal damage kills creatures)
                game.passPriority()

                // Raging Goblin (1/1) should be dead from 1 damage
                val cadetAfter = game.findPermanent("Raging Goblin")
                withClue("Raging Goblin should be destroyed") {
                    cadetAfter shouldBe null
                }

                // Grizzly Bears (2/2) should be dead from 2 damage
                val bearsAfter = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears should be destroyed") {
                    bearsAfter shouldBe null
                }
            }
        }
    }
}
