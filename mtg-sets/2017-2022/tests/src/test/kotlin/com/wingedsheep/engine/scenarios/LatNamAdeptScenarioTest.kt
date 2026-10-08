package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Lat-Nam Adept (BRO #56) — whenever you draw your second card each turn, put a +1/+1 counter on
 * this creature.
 */
class LatNamAdeptScenarioTest : ScenarioTestBase() {

    private fun TestGame.counters(): Int =
        state.getEntity(findPermanent("Lat-Nam Adept")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun game(drawnAlready: Int): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Lat-Nam Adept")
        .withCardInHand(1, "Divination")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardsDrawnThisTurn(1, drawnAlready)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("drawing the second card of the turn adds a +1/+1 counter") {
            val game = game(drawnAlready = 0)
            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()

            game.counters() shouldBe 1
        }

        test("draws past the second card do not trigger") {
            val game = game(drawnAlready = 2)
            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()

            game.counters() shouldBe 0
        }
    }
}
