package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Comforting Counsel. */
class ComfortingCounselScenarioTest : ScenarioTestBase() {

    private fun growthCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.GROWTH) ?: 0

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Comforting Counsel — growth counters per life gain, +3/+3 anthem at 5+") {
            test("each life gain adds a growth counter; the anthem turns on at five counters") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Comforting Counsel")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 30)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInHand(1, "Venerable Monk") }
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val counsel = game.findPermanent("Comforting Counsel")!!
                val bear = game.findPermanent("Grizzly Bears")!!

                // Gain life four times (Venerable Monk ETB gains 2 each) → four growth counters.
                repeat(4) {
                    game.castSpell(1, "Venerable Monk")
                    game.resolveStack()
                }
                withClue("Four life gains → four growth counters") {
                    growthCounters(game, counsel) shouldBe 4
                }
                withClue("Below five counters: no anthem (Grizzly Bears stays 2/2)") {
                    game.state.projectedState.getPower(bear) shouldBe 2
                    game.state.projectedState.getToughness(bear) shouldBe 2
                }

                // Fifth life gain crosses the threshold.
                game.castSpell(1, "Venerable Monk")
                game.resolveStack()
                withClue("Five growth counters now present") {
                    growthCounters(game, counsel) shouldBe 5
                }
                withClue("At 5+ counters the anthem gives creatures you control +3/+3 (Grizzly Bears 5/5)") {
                    game.state.projectedState.getPower(bear) shouldBe 5
                    game.state.projectedState.getToughness(bear) shouldBe 5
                }
            }
        }
    }
}
