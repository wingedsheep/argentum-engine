package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import io.kotest.matchers.shouldBe

class ContaminatedDrinkScenarioTest : ScenarioTestBase() {
    init {
        for (x in listOf(0, 1, 3, 4)) {
            test("draws X=$x and adds half X rad counters rounded up") {
                val b = scenario().withPlayers().withCardInHand(1, "Contaminated Drink")
                    .withCardOnBattlefield(1, "Island").withCardOnBattlefield(1, "Swamp")
                    .withLandsOnBattlefield(1, "Forest", 6).withCardInLibrary(2, "Forest")
                repeat(16) { b.withCardInLibrary(1, "Forest") }
                val g = b.build()
                g.castXSpell(1, "Contaminated Drink", x).error shouldBe null
                g.resolveStack().all { it.error == null } shouldBe true
                g.handSize(1) shouldBe x
                g.state.getEntity(g.player1Id)?.get<CountersComponent>()?.getCount(CounterType.RAD)
                    .let { it ?: 0 } shouldBe (x + 1) / 2
                g.findCardsInGraveyard(1, "Contaminated Drink").size shouldBe 1
            }
        }
        test("the gained counters wait until the next precombat main phase") {
            val b = scenario().withPlayers().withCardInHand(1, "Contaminated Drink")
                .withCardOnBattlefield(1, "Island").withCardOnBattlefield(1, "Swamp")
                .withLandsOnBattlefield(1, "Forest", 2)
            repeat(20) { b.withCardInLibrary(1, "Grizzly Bears"); b.withCardInLibrary(2, "Forest") }
            val g = b.build()
            g.castXSpell(1, "Contaminated Drink", 2).error shouldBe null; g.resolveStack()
            g.getLifeTotal(1) shouldBe 20
            g.state.stack.size shouldBe 0
            g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            // Move through the rest of this turn before returning to player one's main.
            g.passUntilPhase(Phase.ENDING, Step.END)
            g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            if (g.state.activePlayerId != g.player1Id) {
                g.passUntilPhase(Phase.ENDING, Step.END)
                g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            }
            g.state.activePlayerId shouldBe g.player1Id
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 19
            g.state.getEntity(g.player1Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) shouldBe 0
        }
    }
}
