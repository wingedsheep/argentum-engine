package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Naru Meha, Master Wizard.
 *
 * Card reference:
 * - Naru Meha, Master Wizard {2}{U}{U}
 *   Legendary Creature — Human Wizard 3/3
 *   Flash
 *   When Naru Meha, Master Wizard enters the battlefield, copy target instant or sorcery spell
 *   you control. You may choose new targets for the copy.
 *   Other Wizards you control get +1/+1.
 */
class NaruMehaMasterWizardScenarioTest : ScenarioTestBase() {

    init {
        context("Naru Meha, Master Wizard - ETB copy spell") {

            test("ETB trigger copies target instant spell you control") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Naru Meha, Master Wizard")
                    .withCardInHand(1, "Spark Spray")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Goblin Brigand")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Spark Spray targeting Goblin Brigand (1/2)
                val brigandId = game.findPermanent("Goblin Brigand")!!
                game.castSpell(1, "Spark Spray", brigandId)

                // Spark Spray is on the stack. Cast Naru Meha (has Flash) in response.
                game.castSpell(1, "Naru Meha, Master Wizard")

                // Resolve Naru Meha — she enters the battlefield
                game.resolveStack()

                // ETB trigger fires — should prompt to target an instant or sorcery spell you control
                val sparkSprayOnStack = game.state.stack.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Spark Spray"
                }
                withClue("Spark Spray should still be on the stack") {
                    sparkSprayOnStack shouldNotBe null
                }

                // Select Spark Spray as target for the ETB trigger
                val pendingDecision = game.getPendingDecision()
                withClue("Should have a target selection decision for ETB trigger") {
                    pendingDecision shouldNotBe null
                }
                val targetResult = game.selectTargets(listOf(sparkSprayOnStack!!))
                withClue("Target selection should succeed: ${targetResult.error}") {
                    targetResult.error shouldBe null
                }

                // Resolve the triggered ability — the copy effect fires
                game.resolveStack()

                // Should be prompted for new targets for the copy
                val copyDecision = game.getPendingDecision()
                withClue("Should have a target selection decision for the copy") {
                    copyDecision shouldNotBe null
                }

                // Choose same target (Goblin Brigand) for the copy
                game.selectTargets(listOf(brigandId))

                // Copy resolves (1 damage), then original Spark Spray resolves (1 damage)
                // Goblin Brigand is a 1/2, so 2 total damage destroys it
                game.resolveStack()

                withClue("Goblin Brigand should be destroyed by 2 total damage") {
                    game.findPermanent("Goblin Brigand") shouldBe null
                }

                // Naru Meha should be on the battlefield
                withClue("Naru Meha should be on the battlefield") {
                    game.findPermanent("Naru Meha, Master Wizard") shouldNotBe null
                }
            }
        }
    }
}
