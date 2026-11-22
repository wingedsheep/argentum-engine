package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Burst of Strength (GTC) — put a +1/+1 counter on target creature and untap it. */
class BurstOfStrengthScenarioTest : ScenarioTestBase() {
    init {
        test("puts a +1/+1 counter on a tapped creature and untaps it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardInHand(1, "Burst of Strength")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Burst of Strength", bears).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
        }
    }
}
