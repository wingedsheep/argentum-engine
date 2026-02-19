package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Aven Liberator.
 *
 * Card reference:
 * - Aven Liberator ({2}{W}{W}): Creature — Bird Soldier 2/3
 *   Flying
 *   Morph {3}{W}{W}
 *   When Aven Liberator is turned face up, choose a color.
 *   Target creature you control gains protection from the chosen color until end of turn.
 */
class AvenLiberatorScenarioTest : ScenarioTestBase() {

    init {
        context("Aven Liberator") {

            test("turning face up grants protection from chosen color to target creature") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Aven Liberator")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Aven Liberator face-down for {3}
                val avenCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Aven Liberator"
                }
                val castResult = game.execute(CastSpell(game.player1Id, avenCardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Turn face up (pays morph cost {3}{W}{W})
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Triggered ability fires — select target creature (Grizzly Bears)
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val targetResult = game.selectTargets(listOf(bearsId))
                withClue("Target selection should succeed: ${targetResult.error}") {
                    targetResult.error shouldBe null
                }

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // Handle color choice decision — choose red
                val decision = game.getPendingDecision()
                withClue("Should have a color choice decision") {
                    decision.shouldBeInstanceOf<ChooseColorDecision>()
                }
                game.submitDecision(
                    ColorChosenResponse((decision as ChooseColorDecision).id, Color.RED)
                )

                // Grizzly Bears should now have protection from red in projected state
                val projected = StateProjector().project(game.state)
                withClue("Grizzly Bears should have protection from red") {
                    projected.getKeywords(bearsId) shouldContain "PROTECTION_FROM_RED"
                }
            }

            test("can target itself with the turn-face-up trigger") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Aven Liberator")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val avenCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Aven Liberator"
                }
                game.execute(CastSpell(game.player1Id, avenCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Turn face up
                game.execute(TurnFaceUp(game.player1Id, faceDownId))

                // Target the Aven Liberator itself
                val targetResult = game.selectTargets(listOf(faceDownId))
                withClue("Targeting self should succeed: ${targetResult.error}") {
                    targetResult.error shouldBe null
                }

                // Resolve triggered ability
                game.resolveStack()

                // Choose blue
                val decision = game.getPendingDecision() as ChooseColorDecision
                game.submitDecision(ColorChosenResponse(decision.id, Color.BLUE))

                // Aven Liberator should have protection from blue
                val projected = StateProjector().project(game.state)
                withClue("Aven Liberator should have protection from blue") {
                    projected.getKeywords(faceDownId) shouldContain "PROTECTION_FROM_BLUE"
                }
            }
        }
    }
}
