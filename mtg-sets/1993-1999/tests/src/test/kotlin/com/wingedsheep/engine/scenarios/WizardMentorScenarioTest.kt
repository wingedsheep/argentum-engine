package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Wizard Mentor (Urza's Saga)
 * "{T}: Return this creature and target creature you control to their owner's hand."
 */
class WizardMentorScenarioTest : ScenarioTestBase() {

    init {
        context("Wizard Mentor") {

            test("returns itself and the targeted creature you control to hand") {
                val game = scenario()
                    .withPlayers("Mentor", "Opponent")
                    .withCardOnBattlefield(1, "Wizard Mentor", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mentor = game.findPermanent("Wizard Mentor")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Wizard Mentor")!!.script.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = mentor,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.findPermanent("Wizard Mentor") shouldBe null
                game.findPermanent("Grizzly Bears") shouldBe null
                game.isInHand(1, "Wizard Mentor") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }

            test("cannot target a creature an opponent controls") {
                val game = scenario()
                    .withPlayers("Mentor", "Opponent")
                    .withCardOnBattlefield(1, "Wizard Mentor", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mentor = game.findPermanent("Wizard Mentor")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Wizard Mentor")!!.script.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = mentor,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, bears))
                    )
                )
                (result.error != null) shouldBe true
            }
        }
    }
}
