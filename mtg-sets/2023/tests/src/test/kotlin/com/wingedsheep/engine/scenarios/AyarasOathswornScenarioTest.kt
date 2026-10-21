package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ayara's Oathsworn (MAT #11): the "exactly four" check runs after the intervening-if counter is
 * put on it, so the third-to-fourth counter tutors while earlier hits only grow it.
 */
class AyarasOathswornScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(startingCounters: Int): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Ayara's Oathsworn")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val oathsworn = game.findPermanents("Ayara's Oathsworn").first()
        if (startingCounters > 0) seed(game, oathsworn, startingCounters)
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Ayara's Oathsworn" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        return game to oathsworn
    }

    init {
        test("with three counters, the fourth counter lands and it searches for a card") {
            val (game, oathsworn) = board(3)
            var guard = 0
            while (game.state.pendingDecision !is SelectCardsDecision && guard++ < 20) game.resolveStack()

            withClue("it dealt 5 combat damage and now has exactly four counters") {
                game.getLifeTotal(2) shouldBe 15
                counters(game, oathsworn) shouldBe 4
            }
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
        }

        test("with no counters, it only gets a +1/+1 counter and does not search") {
            val (game, oathsworn) = board(0)
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.getLifeTotal(2) shouldBe 18
            counters(game, oathsworn) shouldBe 1
            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
        }

        test("with four counters, the ability does not trigger") {
            val (game, oathsworn) = board(4)
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.getLifeTotal(2) shouldBe 14
            counters(game, oathsworn) shouldBe 4
            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
        }
    }
}
