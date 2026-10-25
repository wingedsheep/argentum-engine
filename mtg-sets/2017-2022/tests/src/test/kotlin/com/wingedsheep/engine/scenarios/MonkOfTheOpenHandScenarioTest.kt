package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Monk of the Open Hand (Adventures in the Forgotten Realms #25).
 *
 * Flurry of Blows — Whenever you cast your second spell each turn, put a +1/+1 counter on this creature.
 *
 * Proves the first spell of the turn does nothing, the second adds exactly one counter, and a
 * third spell adds no more.
 */
class MonkOfTheOpenHandScenarioTest : ScenarioTestBase() {

    init {
        test("only the second spell each turn puts a +1/+1 counter on the Monk") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Monk of the Open Hand")
                .withCardsInHand(1, "Lightning Bolt", 3)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val monk = game.findPermanent("Monk of the Open Hand")!!
            fun counters() = game.state.getEntity(monk)
                ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
            game.resolveStack()
            withClue("the first spell doesn't trigger Flurry of Blows") {
                counters() shouldBe 0
            }

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
            game.resolveStack()
            withClue("the second spell puts one +1/+1 counter on the Monk") {
                counters() shouldBe 1
            }

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
            game.resolveStack()
            withClue("the third spell doesn't trigger again") {
                counters() shouldBe 1
            }
        }
    }
}
