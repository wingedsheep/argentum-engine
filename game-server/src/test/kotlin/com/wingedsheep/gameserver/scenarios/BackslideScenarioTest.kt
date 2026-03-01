package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Backslide:
 * {1}{U}
 * Instant
 * Turn target creature with a morph ability face down.
 * Cycling {U}
 *
 * Cards used:
 * - Backslide ({1}{U} Instant)
 * - Battering Craghorn ({3}{R}, 3/1 First Strike, Morph {1}{R})
 */
class BackslideScenarioTest : ScenarioTestBase() {

    init {
        context("Backslide spell effect") {

            test("turns a face-up morph creature face down") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Battering Craghorn")
                    .withCardInHand(1, "Backslide")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Battering Craghorn face-down for {3}
                val craghornCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                val castResult = game.execute(CastSpell(game.player1Id, craghornCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Turn it face up by paying morph cost {1}{R}
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId))
                withClue("Turn face up should succeed") {
                    turnUpResult.error shouldBe null
                }

                // Verify it's face-up now
                withClue("Creature should be face-up") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe false
                }
                withClue("Creature should still have MorphDataComponent") {
                    game.state.getEntity(faceDownId)?.has<MorphDataComponent>() shouldBe true
                }

                // Now cast Backslide targeting the face-up Craghorn
                val castBackslide = game.castSpell(1, "Backslide", faceDownId)
                withClue("Cast Backslide should succeed: ${castBackslide.error}") {
                    castBackslide.error shouldBe null
                }
                game.resolveStack()

                // Creature should be face-down again
                withClue("Creature should be face-down after Backslide") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe true
                }
            }

            test("cannot target a face-down morph creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Battering Craghorn")
                    .withCardInHand(1, "Backslide")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Battering Craghorn face-down for {3}
                val craghornCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                val castResult = game.execute(CastSpell(game.player1Id, craghornCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Try to cast Backslide targeting the face-down creature — should fail
                val castBackslide = game.castSpell(1, "Backslide", faceDownId)
                withClue("Should not be able to target a creature that is already face-down") {
                    castBackslide.error shouldNotBe null
                }
            }

            test("cannot target a creature without morph") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Backslide")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Try to cast Backslide targeting Grizzly Bears (no morph) — should fail
                val castResult = game.castSpell(1, "Backslide", bearsId)
                withClue("Should not be able to target a creature without morph") {
                    castResult.error shouldNotBe null
                }
            }

            test("can be cycled for {U}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Backslide")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cycle Backslide
                val cycleResult = game.cycleCard(1, "Backslide")
                withClue("Cycling should succeed: ${cycleResult.error}") {
                    cycleResult.error shouldBe null
                }

                // Hand size should be the same (discard Backslide -1, draw +1)
                game.handSize(1) shouldBe initialHandSize

                // Backslide should be in graveyard
                game.isInGraveyard(1, "Backslide") shouldBe true
            }
        }
    }
}
