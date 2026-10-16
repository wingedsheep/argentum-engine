package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class StrongTheBrutishThespianScenarioTest : ScenarioTestBase() {
    private val strong = "Strong, the Brutish Thespian"

    private fun TestGame.rads(player: Int = 1): Int =
        state.getEntity(if (player == 1) player1Id else player2Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0

    init {
        test("enrage: dealt damage gives its controller three rad counters and Strong three +1/+1 counters") {
            val g = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, strong)
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val strongId = g.findPermanent(strong)!!
            // Its own controller's spell doesn't trigger ward.
            g.castSpell(1, "Shock", strongId).error shouldBe null
            g.resolveStack()
            g.state.getEntity(strongId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            g.rads(1) shouldBe 3
            g.rads(2) shouldBe 0
        }

        test("you gain life rather than lose life from radiation, and still remove the rad counters") {
            val b = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, strong)
                .withCardInLibrary(2, "Forest")
            listOf("Grizzly Bears", "Forest", "Grizzly Bears").forEach { b.withCardInLibrary(1, it) }
            val g = b.build()
            g.state = g.state.copy(phase = Phase.BEGINNING, step = Step.DRAW)
            g.state = g.state.updateEntity(g.player1Id) { it.with(CountersComponent(mapOf(CounterType.RAD to 3))) }
            g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 22
            g.rads(1) shouldBe 1
        }
    }
}
