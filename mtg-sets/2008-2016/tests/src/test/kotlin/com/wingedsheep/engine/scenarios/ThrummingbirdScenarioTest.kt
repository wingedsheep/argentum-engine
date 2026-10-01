package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Thrummingbird (SOM #47, reprinted ONE #72) — {1}{U} 1/1 Phyrexian Bird Horror, flying.
 *
 *   Whenever this creature deals combat damage to a player, proliferate.
 */
class ThrummingbirdScenarioTest : ScenarioTestBase() {

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun addCounter(game: TestGame, id: EntityId, type: CounterType) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, 1))
        }
    }

    private fun attackUnblocked(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Thrummingbird" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers().error shouldBe null
        var guard = 0
        while (game.state.pendingDecision == null && guard++ < 20) {
            game.passPriority().error shouldBe null
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Thrummingbird")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("combat damage to a player proliferates the chosen player and permanent") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            addCounter(game, giant, CounterType.PLUS_ONE_PLUS_ONE)
            addCounter(game, game.player2Id, CounterType.POISON)

            attackUnblocked(game)

            game.getLifeTotal(2) shouldBe 19
            game.selectCards(listOf(game.player2Id, giant)).error shouldBe null
            game.resolveStack()

            count(game, game.player2Id, CounterType.POISON) shouldBe 2
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("proliferate may choose nothing") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            addCounter(game, giant, CounterType.PLUS_ONE_PLUS_ONE)
            addCounter(game, game.player2Id, CounterType.POISON)

            attackUnblocked(game)
            game.skipSelection().error shouldBe null
            game.resolveStack()

            count(game, game.player2Id, CounterType.POISON) shouldBe 1
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
