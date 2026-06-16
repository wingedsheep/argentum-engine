package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Atog (ATQ).
 *
 * {1}{R} Creature — Atog, 1/2
 * "Sacrifice an artifact: This creature gets +2/+2 until end of turn."
 */
class AtogScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Atog") {

            test("sacrificing an artifact pumps Atog +2/+2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Atog", summoningSickness = false)
                    // Millstone is an artifact we control to feed the sacrifice cost.
                    .withCardOnBattlefield(1, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val atogId = game.findPermanent("Atog")!!
                val artifactId = game.findPermanent("Millstone")!!
                val ability = cardRegistry.getCard("Atog")!!.script.activatedAbilities[0]

                // Baseline 1/2.
                stateProjector.project(game.state).getPower(atogId) shouldBe 1
                stateProjector.project(game.state).getToughness(atogId) shouldBe 2

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = atogId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(artifactId))
                    )
                )
                withClue("Activating Atog's pump should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The sacrificed artifact should be gone") {
                    game.isOnBattlefield("Millstone") shouldBe false
                }

                val projected = stateProjector.project(game.state)
                withClue("Atog should be 3/4 after +2/+2") {
                    projected.getPower(atogId) shouldBe 3
                    projected.getToughness(atogId) shouldBe 4
                }
            }
        }
    }
}
