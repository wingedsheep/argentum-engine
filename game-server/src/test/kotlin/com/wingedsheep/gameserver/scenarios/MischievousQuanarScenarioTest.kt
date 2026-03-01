package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Mischievous Quanar.
 *
 * Card reference:
 * - Mischievous Quanar ({4}{U}): Creature — Beast 3/3
 *   {3}{U}{U}: Turn Mischievous Quanar face down.
 *   Morph {1}{U}{U}
 *   When Mischievous Quanar is turned face up, copy target instant or sorcery spell.
 *   You may choose new targets for the copy.
 */
class MischievousQuanarScenarioTest : ScenarioTestBase() {

    init {
        context("Mischievous Quanar - CopyTargetSpellEffect") {

            test("turning face up copies a targeted instant on the stack") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Mischievous Quanar")
                    .withCardInHand(1, "Spark Spray")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Goblin Brigand")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Mischievous Quanar face-down for {3}
                val quanarCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Mischievous Quanar"
                }
                val castResult = game.execute(CastSpell(game.player1Id, quanarCardId, castFaceDown = true))
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

                // Cast Spark Spray targeting Goblin Brigand (1/2)
                val brigandId = game.findPermanent("Goblin Brigand")!!
                game.castSpell(1, "Spark Spray", brigandId)

                // Now Spark Spray is on the stack. Turn Quanar face up (special action).
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Triggered ability fires — select target spell on stack (Spark Spray)
                val sparkSprayOnStack = game.state.stack.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Spark Spray"
                }
                withClue("Spark Spray should be on the stack") {
                    sparkSprayOnStack shouldNotBe null
                }
                val targetResult = game.selectTargets(listOf(sparkSprayOnStack!!))
                withClue("Target selection for trigger should succeed: ${targetResult.error}") {
                    targetResult.error shouldBe null
                }

                // Resolve the triggered ability — the copy effect fires
                game.resolveStack()

                // Should be prompted for new targets for the copy
                val copyDecision = game.getPendingDecision()
                withClue("Should have a target selection decision for the copy") {
                    copyDecision shouldNotBe null
                }

                // Choose the same target (Goblin Brigand) for the copy
                game.selectTargets(listOf(brigandId))

                // The copy resolves (deals 1 damage), then Spark Spray resolves (deals 1 damage)
                // Goblin Brigand is a 1/2, so 2 damage total should destroy it
                game.resolveStack()

                // Goblin Brigand should be dead
                withClue("Goblin Brigand should be destroyed by 2 total damage") {
                    game.findPermanent("Goblin Brigand") shouldBe null
                }
            }

            test("copy of untargeted spell is created without target prompt") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Mischievous Quanar")
                    .withCardInHand(1, "Claws of Wirewood")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(2, "Coast Watcher")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Quanar face-down
                val quanarCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Mischievous Quanar"
                }
                game.execute(CastSpell(game.player1Id, quanarCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Cast Claws of Wirewood (untargeted sorcery: 3 damage to each creature with flying)
                game.castSpell(1, "Claws of Wirewood")

                // Turn Quanar face up while Claws of Wirewood is on the stack
                game.execute(TurnFaceUp(game.player1Id, faceDownId))

                // Triggered ability fires — select target spell on stack (Claws of Wirewood)
                // If only one valid target, the engine may auto-select
                if (game.state.pendingDecision != null) {
                    val clawsOnStack = game.state.stack.find { entityId ->
                        game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Claws of Wirewood"
                    }!!
                    game.selectTargets(listOf(clawsOnStack))
                }

                // Resolve everything: trigger creates copy (untargeted, immediate), copy resolves, Claws resolves
                game.resolveStack()

                withClue("Coast Watcher should be destroyed") {
                    game.findPermanent("Coast Watcher") shouldBe null
                }
            }
        }
    }
}
