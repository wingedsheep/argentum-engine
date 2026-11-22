package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Nimbus Swimmer (GTC) — flying; enters with X +1/+1 counters. */
class NimbusSwimmerScenarioTest : ScenarioTestBase() {
    init {
        test("cast with X = 3 enters as a 3/3 flier") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Nimbus Swimmer")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Nimbus Swimmer", 3).error shouldBe null
            game.resolveStack()

            val swimmer = game.findPermanent("Nimbus Swimmer")!!
            game.state.getEntity(swimmer)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            val projected = game.state.projectedState
            projected.getPower(swimmer) shouldBe 3
            projected.getToughness(swimmer) shouldBe 3
            projected.hasKeyword(swimmer, Keyword.FLYING) shouldBe true
        }
    }
}
