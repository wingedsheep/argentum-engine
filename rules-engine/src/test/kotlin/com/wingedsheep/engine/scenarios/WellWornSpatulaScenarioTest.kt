package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Well-Worn Spatula (HOB) — {1} Artifact — Equipment.
 *
 * "When this Equipment enters, you gain 2 life.
 *  Equipped creature gets +1/+1.
 *  Equip {1}"
 *
 * Covers the ETB life gain, that the +1/+1 applies only once attached (not merely by being on the
 * battlefield), and that it follows the Equipment when it is moved to another creature.
 */
class WellWornSpatulaScenarioTest : ScenarioTestBase() {

    init {
        context("Well-Worn Spatula") {

            test("its ETB gains the controller 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Well-Worn Spatula")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Well-Worn Spatula").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the controller gained 2 life; the opponent did not") {
                    game.getLifeTotal(1) shouldBe 22
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("the Equipment is on the battlefield") {
                    game.isOnBattlefield("Well-Worn Spatula") shouldBe true
                }
            }

            test("the +1/+1 applies only to the creature it is equipped to") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Well-Worn Spatula")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spatula = game.findPermanent("Well-Worn Spatula")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val equip = cardRegistry.requireCard("Well-Worn Spatula")
                    .activatedAbilities.single { it.isEquipAbility }.id

                withClue("unattached, the Equipment buffs nobody") {
                    game.state.projectedState.getPower(courser) shouldBe 3
                    game.state.projectedState.getPower(lions) shouldBe 1
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = spatula, abilityId = equip,
                        targets = listOf(ChosenTarget.Permanent(courser))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the Equipment is attached to the Courser") {
                    game.state.getEntity(spatula)?.get<AttachedToComponent>()?.targetId shouldBe courser
                }
                withClue("only the equipped creature gets +1/+1") {
                    game.state.projectedState.getPower(courser) shouldBe 4
                    game.state.projectedState.getToughness(courser) shouldBe 4
                    game.state.projectedState.getPower(lions) shouldBe 1
                    game.state.projectedState.getToughness(lions) shouldBe 1
                }
            }
        }
    }
}
