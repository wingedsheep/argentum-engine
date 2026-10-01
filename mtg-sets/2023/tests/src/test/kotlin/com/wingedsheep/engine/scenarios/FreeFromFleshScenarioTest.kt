package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Free from Flesh (ONE #131) — {R} Instant.
 *
 * "Target creature gets +2/+2 until end of turn. Put two oil counters on it."
 */
class FreeFromFleshScenarioTest : ScenarioTestBase() {

    init {
        test("opponent's creature gets +2/+2 and two oil counters") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Free from Flesh")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Free from Flesh", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.OIL) shouldBe 2
        }
    }
}
