package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Snarl Song. */
class SnarlSongScenarioTest : ScenarioTestBase() {

    private fun growthCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.GROWTH) ?: 0

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Snarl Song — Converge: two Fractals with X counters each, gain X life") {
            test("three colors spent → two 0/0 Fractals with three +1/+1 counters each, gain 3 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Snarl Song")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // {5}{G} paid with W,W,U,U + G + ... three distinct colors (W, U, G).
                game.castSpell(1, "Snarl Song")
                game.resolveStack()

                val fractals = game.findPermanents("Fractal Token")
                withClue("Two Fractal tokens should have been created") {
                    fractals.size shouldBe 2
                }
                withClue("Three colors spent → three +1/+1 counters on each Fractal") {
                    fractals.forEach { plusOneCounters(game, it) shouldBe 3 }
                }
                withClue("You gain X = 3 life") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }
        }
    }
}
