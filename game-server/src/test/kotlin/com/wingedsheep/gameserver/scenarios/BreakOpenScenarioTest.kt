package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Break Open:
 * {1}{R}
 * Instant
 * Turn target face-down creature an opponent controls face up.
 *
 * Cards used:
 * - Break Open ({1}{R} Instant)
 * - Battering Craghorn ({3}{R}, 3/1 First Strike, Morph {1}{R})
 */
class BreakOpenScenarioTest : ScenarioTestBase() {

    init {
        context("Break Open spell effect") {

            test("turns an opponent's face-down creature face up") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Battering Craghorn")
                    .withCardInHand(1, "Break Open")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(2, "Mountain", 4)
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

                // Switch to Player 1's main phase
                game.state = game.state.copy(
                    activePlayerId = game.player1Id,
                    priorityPlayerId = game.player1Id
                )

                // Player 1 casts Break Open targeting the face-down creature
                val castBreakOpen = game.castSpell(1, "Break Open", faceDownId)
                withClue("Cast Break Open should succeed: ${castBreakOpen.error}") {
                    castBreakOpen.error shouldBe null
                }
                game.resolveStack()

                // The creature should now be face-up
                withClue("Creature should be face-up after Break Open") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe false
                }

                // It should be revealed as Battering Craghorn
                withClue("Creature should be Battering Craghorn") {
                    game.state.getEntity(faceDownId)?.get<CardComponent>()?.name shouldBe "Battering Craghorn"
                }
            }

            test("cannot target own face-down creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Battering Craghorn")
                    .withCardInHand(1, "Break Open")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Battering Craghorn face-down
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

                // Try to cast Break Open targeting own face-down creature — should fail
                val castBreakOpen = game.castSpell(1, "Break Open", faceDownId)
                withClue("Should not be able to target own face-down creature") {
                    castBreakOpen.error shouldNotBe null
                }
            }

            test("cannot target a face-up creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Break Open")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Try to cast Break Open targeting face-up creature — should fail
                val castResult = game.castSpell(1, "Break Open", bearsId)
                withClue("Should not be able to target a face-up creature") {
                    castResult.error shouldNotBe null
                }
            }
        }
    }
}
