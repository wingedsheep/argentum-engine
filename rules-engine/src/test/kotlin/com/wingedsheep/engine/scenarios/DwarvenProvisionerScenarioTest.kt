package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dwarven Provisioner (HOB #9) — {1}{W} Creature — Dwarf Citizen 2/2.
 * "{3}{W}: Creatures you control get +1/+1 until end of turn."
 *
 * A team pump has to reach every creature you control *including the Provisioner itself*, and
 * has to stop at the battlefield boundary — an opponent's creature must not move.
 */
class DwarvenProvisionerScenarioTest : ScenarioTestBase() {

    init {
        context("Dwarven Provisioner") {

            test("the ability pumps every creature you control and no others") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dwarven Provisioner")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val provisioner = game.findPermanent("Dwarven Provisioner")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val theirBears = game.findPermanent("Grizzly Bears")!!
                val pump = cardRegistry.requireCard("Dwarven Provisioner").activatedAbilities.single().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = provisioner, abilityId = pump)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the Provisioner pumps itself too — 'creatures you control' includes it") {
                    game.state.projectedState.getPower(provisioner) shouldBe 3
                    game.state.projectedState.getToughness(provisioner) shouldBe 3
                }
                withClue("your other creature is pumped") {
                    game.state.projectedState.getPower(courser) shouldBe 4
                    game.state.projectedState.getToughness(courser) shouldBe 4
                }
                withClue("the opponent's creature is untouched") {
                    game.state.projectedState.getPower(theirBears) shouldBe 2
                    game.state.projectedState.getToughness(theirBears) shouldBe 2
                }
            }
        }
    }
}
