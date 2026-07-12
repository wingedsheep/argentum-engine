package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Lightning Wolf (VOW #168) — {3}{R} Creature — Wolf, 4/3.
 *
 *   {1}{R}: This creature gains first strike until end of turn. Activate only as a sorcery.
 *
 * Exercises the mana-cost activated ability that grants first strike to itself.
 */
class LightningWolfScenarioTest : ScenarioTestBase() {

    init {
        context("Lightning Wolf — {1}{R}: gain first strike") {

            test("activating the ability grants first strike until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lightning Wolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Lightning Wolf")!!
                val abilityId = cardRegistry.getCard("Lightning Wolf")!!.activatedAbilities.first().id

                withClue("Lightning Wolf does not start with first strike") {
                    game.state.projectedState.hasKeyword(wolf, Keyword.FIRST_STRIKE) shouldBe false
                }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wolf,
                        abilityId = abilityId
                    )
                )
                withClue("Activating the ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Lightning Wolf gains first strike until end of turn") {
                    game.state.projectedState.hasKeyword(wolf, Keyword.FIRST_STRIKE) shouldBe true
                }
            }
        }
    }
}
