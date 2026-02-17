package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
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
 * Scenario tests for Ascending Aven.
 *
 * Ascending Aven: {2}{U}{U} 3/2 Bird Soldier
 * "Flying. Ascending Aven can block only creatures with flying. Morph {2}{U}"
 *
 * Key rule (Rule 707.2): face-down creatures have no abilities. While face-down,
 * Ascending Aven is a vanilla 2/2 and its blocking restriction does not apply.
 */
class AscendingAvenScenarioTest : ScenarioTestBase() {

    init {
        context("Ascending Aven face-down loses its blocking restriction (Rule 707.2)") {

            test("face-down Ascending Aven can block a non-flying creature") {
                // Set up on player 1's turn so they can cast the morph
                val game = scenario()
                    .withPlayers("Morpher", "Attacker")
                    .withCardInHand(1, "Ascending Aven")
                    .withLandsOnBattlefield(1, "Island", 3)    // exactly enough for morph {3}
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 non-flying attacker
                    .withCardInLibrary(1, "Island")            // prevent draw-from-empty
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Ascending Aven face-down for {3}
                val avenCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Ascending Aven"
                }
                val castResult = game.execute(CastSpell(game.player1Id, avenCardId, castFaceDown = true))
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

                // Advance past player 1's combat (morph has summoning sickness, nothing to attack),
                // then to player 2's DECLARE_ATTACKERS step
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Player 2 attacks with Grizzly Bears (non-flying)
                val attackResult = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(bearsId to game.player1Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Player 1 blocks with face-down Ascending Aven — should succeed because
                // face-down creatures have no abilities (Rule 707.2), so the
                // "can block only creatures with flying" restriction is gone
                val blockResult = game.execute(
                    DeclareBlockers(game.player1Id, mapOf(faceDownId!! to listOf(bearsId)))
                )
                withClue("Face-down Ascending Aven should be able to block non-flying creature: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("face-up Ascending Aven cannot block a non-flying creature") {
                // 3 for morph cost + 3 for turn face-up {2}{U} = 6 Islands
                val game = scenario()
                    .withPlayers("Morpher", "Attacker")
                    .withCardInHand(1, "Ascending Aven")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val avenCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Ascending Aven"
                }
                game.execute(CastSpell(game.player1Id, avenCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Turn face-up (pays morph cost {2}{U})
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }
                withClue("Ascending Aven should be face-up") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe false
                }

                // Advance past player 1's combat, then to player 2's DECLARE_ATTACKERS
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val bearsId = game.findPermanent("Grizzly Bears")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(bearsId to game.player1Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Face-up Ascending Aven has the "can block only flying" restriction — should fail
                val blockResult = game.execute(
                    DeclareBlockers(game.player1Id, mapOf(faceDownId to listOf(bearsId)))
                )
                withClue("Face-up Ascending Aven should NOT be able to block a non-flying creature") {
                    blockResult.error shouldNotBe null
                }
            }
        }
    }
}
