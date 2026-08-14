package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class IngeniousProdigyScenarioTest : ScenarioTestBase() {

    init {
        test("enters with X +1/+1 counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Ingenious Prodigy")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Ingenious Prodigy", xValue = 2).error shouldBe null
            game.resolveStack()

            val prodigy = game.findPermanent("Ingenious Prodigy")!!
            fun counterCount(): Int = game.state.getEntity(prodigy)
                ?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            counterCount() shouldBe 2
        }

        test("may remove a +1/+1 counter at upkeep to draw") {
            var builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ingenious Prodigy")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
            repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
            val game = builder.build()

            val prodigy = game.findPermanent("Ingenious Prodigy")!!
            game.state = game.state.updateEntity(prodigy) {
                it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
            }

            fun counterCount(): Int = game.state.getEntity(prodigy)
                ?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            counterCount() shouldBe 1
            game.handSize(1) shouldBe 1
        }
    }
}
