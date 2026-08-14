package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Scheming Silvertongue. */
class SchemingSilvertongueScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(name: String): Int {
        val id = findPermanent(name) ?: return 0
        return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Scheming Silvertongue — prepared at second main phase if you gained 2+ life") {
            test("becomes prepared when you gained 2 or more life this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Scheming Silvertongue", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.END_COMBAT)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(2))
                }

                val silvertongue = game.findPermanent("Scheming Silvertongue")!!
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("gained 2+ life → second-main trigger makes it prepared") {
                    game.state.getEntity(silvertongue)?.get<PreparedComponent>() shouldNotBe null
                }
            }

            test("does NOT become prepared when you gained only 1 life this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Scheming Silvertongue", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.END_COMBAT)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(1))
                }

                val silvertongue = game.findPermanent("Scheming Silvertongue")!!
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("only 1 life gained → intervening-if fails, stays unprepared") {
                    game.state.getEntity(silvertongue)?.get<PreparedComponent>() shouldBe null
                }
            }
        }
    }
}
