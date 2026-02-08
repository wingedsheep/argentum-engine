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
 * Scenario tests for Fallen Cleric.
 *
 * Card reference:
 * - Fallen Cleric ({4}{B}): Creature — Zombie Cleric, 4/2
 *   Protection from Clerics
 *   Morph {4}{B}
 */
class FallenClericScenarioTest : ScenarioTestBase() {

    init {
        context("Fallen Cleric protection from Clerics") {

            test("cannot be blocked by Clerics") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Fallen Cleric")     // 4/2 pro Clerics
                    .withCardOnBattlefield(2, "Battlefield Medic") // 1/1 Cleric
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Fallen Cleric" to 2))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to block with a Cleric — should fail
                val blockResult = game.declareBlockers(mapOf("Battlefield Medic" to listOf("Fallen Cleric")))
                withClue("Cleric should not be able to block Fallen Cleric") {
                    blockResult.error shouldNotBe null
                }
            }

            test("can be blocked by non-Clerics") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Fallen Cleric") // 4/2 pro Clerics
                    .withCardOnBattlefield(2, "Elvish Warrior") // 2/3 Elf Warrior (not a Cleric)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Fallen Cleric" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with non-Cleric — should succeed
                val blockResult = game.declareBlockers(mapOf("Elvish Warrior" to listOf("Fallen Cleric")))
                withClue("Non-Cleric should be able to block Fallen Cleric: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Fallen Cleric (4/2) kills Elvish Warrior (2/3), but takes 2 damage and dies (2 toughness)
                withClue("Fallen Cleric should die from 2 damage") {
                    game.isOnBattlefield("Fallen Cleric") shouldBe false
                }
                withClue("Elvish Warrior should die from 4 damage") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }
            }

            test("takes no combat damage from Clerics when both in combat") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Glory Seeker")      // 2/2 non-Cleric attacker
                    .withCardOnBattlefield(2, "Fallen Cleric")     // 4/2 pro Clerics, defending
                    .withCardOnBattlefield(1, "Battlefield Medic") // 1/1 Cleric attacking
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Battlefield Medic" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block the Cleric attacker with Fallen Cleric
                game.declareBlockers(mapOf("Fallen Cleric" to listOf("Battlefield Medic")))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Fallen Cleric has protection from Clerics, so Battlefield Medic's 1 damage is prevented
                // Fallen Cleric deals 4 to Battlefield Medic, killing it
                withClue("Fallen Cleric should survive (protection prevents Cleric damage)") {
                    game.isOnBattlefield("Fallen Cleric") shouldBe true
                }
                withClue("Battlefield Medic should die from 4 damage") {
                    game.isInGraveyard(1, "Battlefield Medic") shouldBe true
                }
            }
        }

        context("Fallen Cleric morph") {

            test("can be cast face-down and turned face-up") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Fallen Cleric")
                    .withLandsOnBattlefield(1, "Swamp", 8) // 3 for morph + 5 for unmorph {4}{B}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Find the card in hand and cast face-down
                val hand = game.state.getHand(game.player1Id)
                val cardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Fallen Cleric"
                }!!
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast morph should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature on battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Should have a face-down creature on battlefield") {
                    (faceDownId != null) shouldBe true
                }

                // Turn face-up by paying {4}{B}
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Should now be Fallen Cleric
                withClue("Fallen Cleric should be face-up on battlefield") {
                    game.isOnBattlefield("Fallen Cleric") shouldBe true
                }
            }
        }
    }
}
