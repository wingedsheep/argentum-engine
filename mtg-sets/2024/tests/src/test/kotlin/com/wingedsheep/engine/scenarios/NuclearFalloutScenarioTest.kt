package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nuclear Fallout (PIP #47): every creature gets twice -X/-X until end of turn, and every
 * player — the caster included — gets X rad counters.
 */
class NuclearFalloutScenarioTest : ScenarioTestBase() {
    init {
        test("X=1 gives every creature -2/-2 and each player one rad counter") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nuclear Fallout")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Serra Angel")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val angel = game.findPermanent("Serra Angel")!!

            game.castXSpell(1, "Nuclear Fallout", xValue = 1).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.state.projectedState.getPower(angel) shouldBe 2
            game.state.projectedState.getToughness(angel) shouldBe 2

            game.state.getEntity(game.player1Id)?.get<CountersComponent>()
                ?.getCount(CounterType.RAD) shouldBe 1
            game.state.getEntity(game.player2Id)?.get<CountersComponent>()
                ?.getCount(CounterType.RAD) shouldBe 1
        }
    }
}
