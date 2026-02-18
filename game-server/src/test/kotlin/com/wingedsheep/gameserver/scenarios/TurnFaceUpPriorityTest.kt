package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
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
 * Regression test: TurnFaceUp during combat must reset priorityPassedBy.
 *
 * Bug: When the defending player passes priority at DECLARE_BLOCKERS, then the
 * attacking player turns a creature face up (special action), the defender's
 * earlier pass was still recorded in priorityPassedBy. When the attacker then
 * passed, allPlayersPassed() returned true and the game skipped straight to
 * combat damage — the defender never got priority after the morph flip.
 *
 * Fix: TurnFaceUpHandler now calls withPriority() to clear priorityPassedBy.
 */
class TurnFaceUpPriorityTest : ScenarioTestBase() {

    init {
        test("defender gets priority after attacker turns creature face up during declare blockers") {
            // Player 2 (attacker) has a face-down Fallen Cleric that can be turned face up
            // Player 1 (defender) has Snarling Undorak (3/3 Beast with {2}{G}: Beast gets +1/+1)
            // and enough mana to activate the ability.
            //
            // After blockers are declared:
            //   1. Defender passes priority
            //   2. Attacker turns face-down creature face up (TurnFaceUp, special action)
            //   3. Attacker passes priority
            //   4. Defender should get priority (to pump Undorak before damage)
            //
            // Before the fix, step 4 was skipped because priorityPassedBy still contained
            // the defender's pass from step 1.

            val game = scenario()
                .withPlayers("Defender", "Attacker")
                // Defender: Snarling Undorak + enough mana for {2}{G}
                .withCardOnBattlefield(1, "Snarling Undorak")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                // Attacker: lands for morphing (we'll cast face-down in precombat main)
                .withCardInHand(2, "Fallen Cleric")
                .withLandsOnBattlefield(2, "Swamp", 8) // enough for {3} morph + {4}{B} unmorph
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast Fallen Cleric face-down (morph for {3})
            val clericCardId = game.state.getHand(game.player2Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Fallen Cleric"
            }
            val castResult = game.execute(CastSpell(game.player2Id, clericCardId, castFaceDown = true))
            withClue("Cast face-down should succeed: ${castResult.error}") {
                castResult.error shouldBe null
            }
            game.resolveStack()

            val faceDownId = game.state.getBattlefield().find { entityId ->
                game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
            }
            withClue("Face-down creature should be on battlefield") {
                faceDownId shouldNotBe null
            }

            // Advance through P2's rest of turn, P1's full turn, to P2's second turn's combat.
            // This ensures the face-down creature has lost summoning sickness.
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN) // P2's postcombat
            // P2 passes end step → P1's turn begins
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // P1's main
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN) // P1's postcombat
            // P1 passes end step → P2's second turn begins
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS) // P2's combat

            // Verify P2 is the active player on their second turn
            withClue("Player 2 should be active player") {
                game.state.activePlayerId shouldBe game.player2Id
            }

            // Player 2 attacks with face-down creature
            val attackResult = game.execute(
                DeclareAttackers(game.player2Id, mapOf(faceDownId!! to game.player1Id))
            )
            withClue("Attack should succeed: ${attackResult.error}") {
                attackResult.error shouldBe null
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            // Player 1 blocks face-down creature with Snarling Undorak
            val undorakId = game.findPermanent("Snarling Undorak")!!
            val blockResult = game.execute(
                DeclareBlockers(game.player1Id, mapOf(undorakId to listOf(faceDownId)))
            )
            withClue("Block should succeed: ${blockResult.error}") {
                blockResult.error shouldBe null
            }

            // After blockers declared, defender (player 1) has priority.
            // Defender passes priority (choosing not to pump yet).
            withClue("Player 1 (defender) should have priority after declaring blockers") {
                game.state.priorityPlayerId shouldBe game.player1Id
            }
            game.execute(PassPriority(game.player1Id))

            // Attacker (player 2) should now have priority.
            withClue("Player 2 (attacker) should have priority after defender passes") {
                game.state.priorityPlayerId shouldBe game.player2Id
            }

            // Attacker turns face-down creature face up (special action — Fallen Cleric 4/2)
            val turnUpResult = game.execute(TurnFaceUp(game.player2Id, faceDownId))
            withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                turnUpResult.error shouldBe null
            }

            // Attacker retains priority after special action
            withClue("Player 2 (attacker) should retain priority after TurnFaceUp") {
                game.state.priorityPlayerId shouldBe game.player2Id
            }

            // Attacker passes priority
            game.execute(PassPriority(game.player2Id))

            // KEY ASSERTION: Defender (player 1) should now get priority!
            // Before the fix, priorityPassedBy still contained player 1's earlier pass,
            // so allPlayersPassed() returned true and the game advanced to combat damage.
            withClue("Player 1 (defender) should get priority after attacker turns creature face up and passes — must not skip to combat damage") {
                game.state.priorityPlayerId shouldBe game.player1Id
            }

            // Verify we're still in DECLARE_BLOCKERS step (not combat damage)
            withClue("Should still be in DECLARE_BLOCKERS step") {
                game.state.step shouldBe Step.DECLARE_BLOCKERS
            }
        }
    }
}
