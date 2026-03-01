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
 * Tests for Warbreak Trumpeter — morph with X cost ({X}{X}{R}).
 *
 * When turned face up, creates X 1/1 red Goblin creature tokens.
 * The X value is determined by the amount paid in the morph cost.
 */
class WarbreakTrumpeterScenarioTest : ScenarioTestBase() {

    init {
        test("turning Warbreak Trumpeter face up with X=2 creates 2 goblin tokens") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Warbreak Trumpeter")
                .withLandsOnBattlefield(1, "Mountain", 8) // Plenty for {3} cast + {X}{X}{R} morph
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast Warbreak Trumpeter face-down (morph for {3})
            val trumpeterId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Warbreak Trumpeter"
            }
            val castResult = game.execute(CastSpell(game.player1Id, trumpeterId, castFaceDown = true))
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

            // Turn face up with X=2 (cost = {X}{X}{R} with X=2 = 5 mana total)
            val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!, xValue = 2))
            withClue("Turn face-up should succeed (error=${turnUpResult.error})") {
                turnUpResult.error shouldBe null
            }

            // Check if there's something on the stack
            withClue("Stack should have triggered ability") {
                game.state.stack.size shouldBe 1
            }

            // Check the triggered ability on the stack has xValue
            val stackItemId = game.state.stack.first()
            val triggeredComponent = game.state.getEntity(stackItemId)
                ?.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()
            withClue("Triggered ability on stack should have xValue=2") {
                triggeredComponent?.xValue shouldBe 2
            }

            // Resolve the triggered ability (create X tokens)
            game.resolveStack()

            // Should have 2 Goblin tokens
            val goblins = game.findAllPermanents("Goblin Token")
            withClue("Should have 2 Goblin tokens") {
                goblins.size shouldBe 2
            }

            // Warbreak Trumpeter should no longer be face-down
            withClue("Warbreak Trumpeter should be face-up") {
                game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe false
            }
        }

        test("turning Warbreak Trumpeter face up with X=0 creates no tokens") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Warbreak Trumpeter")
                .withLandsOnBattlefield(1, "Mountain", 4) // {3} for cast + {R} for morph with X=0
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast face-down
            val trumpeterId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Warbreak Trumpeter"
            }
            game.execute(CastSpell(game.player1Id, trumpeterId, castFaceDown = true))
            game.resolveStack()

            val faceDownId = game.state.getBattlefield().find { entityId ->
                game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
            }!!

            // Turn face up with X=0 (cost = {0}{0}{R} = 1 mana)
            val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId, xValue = 0))
            withClue("Turn face-up with X=0 should succeed: ${turnUpResult.error}") {
                turnUpResult.error shouldBe null
            }

            // Resolve the triggered ability (create 0 tokens)
            game.resolveStack()

            // Should have no Goblin tokens
            val goblins = game.findAllPermanents("Goblin Token")
            withClue("Should have 0 Goblin tokens with X=0") {
                goblins.size shouldBe 0
            }
        }
    }
}
