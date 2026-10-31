package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Bloom Hulk (WAR) — {3}{G} Creature — Plant Elemental 4/4.
 *
 *   When this creature enters, proliferate.
 */
class BloomHulkScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("entering proliferates only the chosen permanents") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Bloom Hulk")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, giant, 1)
            seed(game, bears, 1)

            game.castSpell(1, "Bloom Hulk").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Bloom Hulk") shouldBe true

            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            plusOnes(game, giant) shouldBe 2
            plusOnes(game, bears) shouldBe 1
        }
    }
}
