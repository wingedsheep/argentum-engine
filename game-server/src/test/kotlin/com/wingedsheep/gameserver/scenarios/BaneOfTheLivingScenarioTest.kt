package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Bane of the Living — morph with X cost ({X}{B}{B}).
 *
 * When turned face up, all creatures get -X/-X until end of turn.
 */
class BaneOfTheLivingScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        test("turning Bane of the Living face up with X=2 gives all creatures -2/-2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bane of the Living")
                .withLandsOnBattlefield(1, "Swamp", 10)
                .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast Bane of the Living face-down
            val baneId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Bane of the Living"
            }
            val castResult = game.execute(CastSpell(game.player1Id, baneId, castFaceDown = true))
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

            // Turn face up with X=2 (cost = {2}{B}{B} = 4 mana)
            val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!, xValue = 2))
            withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                turnUpResult.error shouldBe null
            }

            // Resolve the triggered ability (-X/-X to all creatures)
            game.resolveStack()

            // Glory Seeker (2/2) should get -2/-2 = 0/0 and die (state-based actions)
            val glorySeekers = game.findAllPermanents("Glory Seeker")
            withClue("Glory Seeker should be dead (0/0 from -2/-2)") {
                glorySeekers.size shouldBe 0
            }

            // Bane of the Living (4/3) should be 2/1
            val projected = projector.project(game.state)
            val baneOnBattlefield = game.findAllPermanents("Bane of the Living").firstOrNull()
            withClue("Bane of the Living should still be on battlefield") {
                baneOnBattlefield shouldNotBe null
            }
            withClue("Bane of the Living should be 2/1 after -2/-2") {
                projected.getPower(baneOnBattlefield!!) shouldBe 2
                projected.getToughness(baneOnBattlefield) shouldBe 1
            }
        }

        test("turning Bane of the Living face up with X=0 does nothing to creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bane of the Living")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast face-down
            val baneId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Bane of the Living"
            }
            game.execute(CastSpell(game.player1Id, baneId, castFaceDown = true))
            game.resolveStack()

            val faceDownId = game.state.getBattlefield().find { entityId ->
                game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
            }!!

            // Turn face up with X=0 (cost = {0}{B}{B} = 2 mana)
            val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId, xValue = 0))
            withClue("Turn face-up with X=0 should succeed: ${turnUpResult.error}") {
                turnUpResult.error shouldBe null
            }

            // Resolve the triggered ability (-0/-0 to all creatures — no change)
            game.resolveStack()

            // Glory Seeker should still be alive
            val glorySeekers = game.findAllPermanents("Glory Seeker")
            withClue("Glory Seeker should still be alive with -0/-0") {
                glorySeekers.size shouldBe 1
            }
        }

        test("Bane of the Living -X/-X wears off at end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bane of the Living")
                .withLandsOnBattlefield(1, "Swamp", 10)
                .withCardOnBattlefield(2, "Enormous Baloth") // 7/7 — survives -3/-3
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast face-down
            val baneId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Bane of the Living"
            }
            game.execute(CastSpell(game.player1Id, baneId, castFaceDown = true))
            game.resolveStack()

            val faceDownId = game.state.getBattlefield().find { entityId ->
                game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
            }!!

            // Turn face up with X=3
            game.execute(TurnFaceUp(game.player1Id, faceDownId, xValue = 3))
            game.resolveStack()

            // Enormous Baloth should be 4/4 during this turn
            val projected = projector.project(game.state)
            val balothId = game.findAllPermanents("Enormous Baloth").first()
            withClue("Enormous Baloth should be 4/4 after -3/-3") {
                projected.getPower(balothId) shouldBe 4
                projected.getToughness(balothId) shouldBe 4
            }

            // Advance to cleanup step — the -X/-X should wear off
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)

            val projectedAfter = projector.project(game.state)
            withClue("Enormous Baloth should be 7/7 after end of turn cleanup") {
                projectedAfter.getPower(balothId) shouldBe 7
                projectedAfter.getToughness(balothId) shouldBe 7
            }
        }
    }
}
