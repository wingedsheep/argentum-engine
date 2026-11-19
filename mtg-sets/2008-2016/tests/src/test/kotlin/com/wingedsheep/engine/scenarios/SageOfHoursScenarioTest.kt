package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Sage of Hours — heroic +1/+1 counter, and "Remove all +1/+1 counters from this creature: For
 * each five counters removed this way, take an extra turn after this one." The count is the number
 * the cost removed, divided by five and rounded down (ruling: twelve → two turns, three → none).
 */
class SageOfHoursScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.addPlusOnes(id: EntityId, n: Int) {
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, n))
        }
    }

    private fun TestGame.pendingExtraTurns(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<ExtraTurnsComponent>()?.pending?.size ?: 0

    private fun TestGame.skippedTurns(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<SkipNextTurnComponent>()?.turns ?: 0

    private val extraTurnAbility by lazy {
        cardRegistry.requireCard("Sage of Hours").script.activatedAbilities[0].id
    }

    private fun TestGame.activateWith(counters: Int): EntityId {
        val sage = findPermanent("Sage of Hours")!!
        addPlusOnes(sage, counters)
        execute(ActivateAbility(player1Id, sage, extraTurnAbility)).error shouldBe null
        plusOnes(sage) shouldBe 0
        resolveStack()
        return sage
    }

    private fun sageGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Sage of Hours")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("heroic: a spell targeting the Sage puts a +1/+1 counter on it") {
            val game = sageGame()
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .build()
            val sage = game.findPermanent("Sage of Hours")!!

            game.castSpell(1, "Giant Growth", sage).error shouldBe null
            game.resolveStack()

            game.plusOnes(sage) shouldBe 1
        }

        test("removing twelve counters takes two extra turns") {
            val game = sageGame().build()
            game.activateWith(12)

            game.pendingExtraTurns(game.player1Id) shouldBe 2
            game.skippedTurns(game.player2Id) shouldBe 2
        }

        test("removing three counters takes no extra turn") {
            val game = sageGame().build()
            game.activateWith(3)

            game.pendingExtraTurns(game.player1Id) shouldBe 0
            game.skippedTurns(game.player2Id) shouldBe 0
        }
    }
}
