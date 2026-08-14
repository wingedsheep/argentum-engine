package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Ulna Alley Shopkeep. */
class UlnaAlleyShopkeepScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(name: String): Int {
        val id = findPermanent(name) ?: return 0
        return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Ulna Alley Shopkeep — Infusion conditional buff") {
            test("gets +2/+0 only while you gained life this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ulna Alley Shopkeep", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shopkeep = game.findPermanent("Ulna Alley Shopkeep")!!
                withClue("base 2/3 with no life gained this turn") {
                    game.state.projectedState.getPower(shopkeep) shouldBe 2
                    game.state.projectedState.getToughness(shopkeep) shouldBe 3
                }

                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(1))
                }

                withClue("after gaining life this turn, Infusion grants +2/+0 → 4/3") {
                    game.state.projectedState.getPower(shopkeep) shouldBe 4
                    game.state.projectedState.getToughness(shopkeep) shouldBe 3
                }
            }
        }
    }
}
