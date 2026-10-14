package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Inexorable Tide (SOM #35) — {3}{U}{U} Enchantment.
 *
 *   Whenever you cast a spell, proliferate.
 */
class InexorableTideScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board(activePlayer: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Inexorable Tide")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardInHand(activePlayer, "Grizzly Bears")
        .withLandsOnBattlefield(activePlayer, "Forest", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(activePlayer)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("casting a spell proliferates") {
            val game = board(activePlayer = 1)
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("an opponent's spell does not trigger it") {
            val game = board(activePlayer = 2)
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
