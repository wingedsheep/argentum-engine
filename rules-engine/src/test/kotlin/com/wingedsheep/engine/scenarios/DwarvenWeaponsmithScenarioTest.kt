package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Dwarven Weaponsmith (ATQ #25).
 *
 * "{T}, Sacrifice an artifact: Put a +1/+1 counter on target creature.
 *  Activate only during your upkeep."
 */
class DwarvenWeaponsmithScenarioTest : ScenarioTestBase() {

    // A vanilla 2/2 to receive the +1/+1 counter.
    private val testBear = card("Counter Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    private val stateProjector = StateProjector()

    init {
        cardRegistry.register(testBear)

        context("Dwarven Weaponsmith") {

            test("tap + sacrifice an artifact puts a +1/+1 counter on target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dwarven Weaponsmith", summoningSickness = false)
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Counter Test Bear", summoningSickness = false)
                    .withActivePlayer(1)
                    // Ability is restricted to the controller's upkeep.
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val smithId = game.findPermanent("Dwarven Weaponsmith")!!
                val artifactId = game.findPermanent("Ornithopter")!!
                val bearId = game.findPermanent("Counter Test Bear")!!

                val abilityId = cardRegistry.getCard("Dwarven Weaponsmith")!!
                    .script.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = smithId,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, bearId)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(artifactId))
                    )
                )
                withClue("Activating the +1/+1 ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The sacrificed artifact should be gone from the battlefield") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                }

                val projected = stateProjector.project(game.state)
                withClue("Target Bear should be 3/3 after one +1/+1 counter") {
                    projected.getPower(bearId) shouldBe 3
                    projected.getToughness(bearId) shouldBe 3
                }
            }
        }
    }
}
