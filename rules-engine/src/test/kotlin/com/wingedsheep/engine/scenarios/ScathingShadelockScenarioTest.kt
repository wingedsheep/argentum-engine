package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Scathing Shadelock. */
class ScathingShadelockScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(name: String): Int {
        val id = findPermanent(name) ?: return 0
        return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Scathing Shadelock — becomes prepared at first main phase") {
            test("becomes prepared at the beginning of your first main phase") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Scathing Shadelock", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val shadelock = game.findPermanent("Scathing Shadelock")!!
                withClue("not prepared during upkeep") {
                    game.state.getEntity(shadelock)?.get<PreparedComponent>() shouldBe null
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()

                withClue("the first-main-phase trigger makes it prepared") {
                    game.state.getEntity(shadelock)?.get<PreparedComponent>() shouldNotBe null
                }
            }
        }
    }
}
