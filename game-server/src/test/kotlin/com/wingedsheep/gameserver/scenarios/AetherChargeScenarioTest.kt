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

/**
 * Scenario tests for Aether Charge.
 *
 * Card reference:
 * - Aether Charge ({4}{R}): Enchantment
 *   "Whenever a Beast you control enters the battlefield, you may have it deal 4 damage
 *   to target opponent."
 */
class AetherChargeScenarioTest : ScenarioTestBase() {

    init {
        context("Aether Charge triggers when morph Beast is turned face up") {

            test("turning a Beast morph face up triggers Aether Charge") {
                val game = scenario()
                    .withPlayers("Beast Player", "Opponent")
                    .withCardOnBattlefield(1, "Aether Charge")
                    .withCardInHand(1, "Snarling Undorak") // Beast with morph {1}{G}{G}
                    .withLandsOnBattlefield(1, "Forest", 3) // For morph cast
                    .withLandsOnBattlefield(1, "Mountain", 3) // For unmorph
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Snarling Undorak face-down as a morph
                val hand = game.state.getHand(game.player1Id)
                val undorakCardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Snarling Undorak"
                }!!
                val castResult = game.execute(CastSpell(game.player1Id, undorakCardId, castFaceDown = true))
                withClue("Cast morph should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the morph spell - should NOT trigger Aether Charge (face-down = no subtypes)
                game.resolveStack()

                withClue("Opponent should still be at 20 life (face-down creature is not a Beast)") {
                    game.getLifeTotal(2) shouldBe 20
                }

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Should have a face-down creature on battlefield") {
                    (faceDownId != null) shouldBe true
                }

                // Turn face up - this SHOULD trigger Aether Charge (now it's a Beast)
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Aether Charge trigger: "you may have it deal 4 damage to target opponent"
                // Select target opponent
                game.selectTargets(listOf(game.player2Id))

                // Answer yes to the may ability
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Opponent should have taken 4 damage from Aether Charge trigger") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("can decline Aether Charge trigger when morph is turned face up") {
                val game = scenario()
                    .withPlayers("Beast Player", "Opponent")
                    .withCardOnBattlefield(1, "Aether Charge")
                    .withCardInHand(1, "Snarling Undorak")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val hand = game.state.getHand(game.player1Id)
                val undorakCardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Snarling Undorak"
                }!!
                game.execute(CastSpell(game.player1Id, undorakCardId, castFaceDown = true))
                game.resolveStack()

                // Turn face up
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!
                game.execute(TurnFaceUp(game.player1Id, faceDownId))

                // Select target opponent
                game.selectTargets(listOf(game.player2Id))

                // Decline the may ability
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Opponent should still be at 20 life (declined trigger)") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("does not trigger when non-Beast morph is turned face up") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aether Charge")
                    .withCardInHand(1, "Hystrodon") // Beast - but let's test with a non-Beast
                    .withCardInHand(1, "Ironfist Crusher") // Soldier, not a Beast
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Ironfist Crusher face-down
                val hand = game.state.getHand(game.player1Id)
                val crusherId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Ironfist Crusher"
                }!!
                game.execute(CastSpell(game.player1Id, crusherId, castFaceDown = true))
                game.resolveStack()

                // Turn face up
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!
                game.execute(TurnFaceUp(game.player1Id, faceDownId))

                withClue("Opponent should still be at 20 (Ironfist Crusher is not a Beast)") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("triggers when Beast is cast normally (not morph)") {
                val game = scenario()
                    .withPlayers("Beast Player", "Opponent")
                    .withCardOnBattlefield(1, "Aether Charge")
                    .withCardInHand(1, "Barkhide Mauler") // Beast, no morph
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Barkhide Mauler")
                game.resolveStack()

                // Aether Charge triggers - select target opponent
                game.selectTargets(listOf(game.player2Id))

                // Answer yes to the may ability
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Opponent should have taken 4 damage from Aether Charge") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }
        }
    }
}
