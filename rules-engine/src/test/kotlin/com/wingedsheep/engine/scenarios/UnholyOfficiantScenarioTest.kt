package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Unholy Officiant (VOW #41) — {W} Creature — Vampire Cleric, 1/2.
 *
 *   Vigilance
 *   {4}{W}: Put a +1/+1 counter on this creature.
 *
 * Exercises the printed Vigilance keyword and the mana-only activated ability that adds a
 * +1/+1 counter to itself.
 */
class UnholyOfficiantScenarioTest : ScenarioTestBase() {

    init {
        context("Unholy Officiant") {

            test("has vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Unholy Officiant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val officiant = game.findPermanent("Unholy Officiant")!!
                withClue("Unholy Officiant has vigilance") {
                    game.state.projectedState.hasKeyword(officiant, Keyword.VIGILANCE) shouldBe true
                }
            }

            test("{4}{W}: puts a +1/+1 counter on itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Unholy Officiant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val officiant = game.findPermanent("Unholy Officiant")!!
                val abilityId = cardRegistry.getCard("Unholy Officiant")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = officiant,
                        abilityId = abilityId
                    )
                )
                withClue("Activating the {4}{W} ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("A +1/+1 counter is placed on Unholy Officiant") {
                    game.state.getEntity(officiant)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("Power/toughness reflect the counter (becomes 2/3)") {
                    game.state.projectedState.getPower(officiant) shouldBe 2
                    game.state.projectedState.getToughness(officiant) shouldBe 3
                }
            }
        }
    }
}
