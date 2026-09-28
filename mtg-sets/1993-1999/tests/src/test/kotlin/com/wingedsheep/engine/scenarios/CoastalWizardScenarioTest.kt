package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Coastal Wizard (Portal Second Age). */
class CoastalWizardScenarioTest : ScenarioTestBase() {

    init {
        context("Coastal Wizard") {

            fun activate(game: TestGame): Boolean {
                val wizard = game.findPermanent("Coastal Wizard")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Coastal Wizard")!!.script.activatedAbilities[0].id
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wizard,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, bears))
                    )
                )
                if (result.error != null) return false
                game.resolveStack()
                return true
            }

            test("returns itself and the other creature to their owners' hands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Coastal Wizard", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activate(game) shouldBe true
                withClue("wizard returns to its owner's hand") {
                    game.isInHand(1, "Coastal Wizard") shouldBe true
                }
                withClue("target returns to its owner's hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
            }

            test("cannot be activated after attackers are declared") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Coastal Wizard", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                activate(game) shouldBe false
            }
        }
    }
}
