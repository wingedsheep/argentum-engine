package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Courage in Crisis (WAR #158) — {2}{G} Sorcery.
 *
 *   Put a +1/+1 counter on target creature, then proliferate.
 */
class CourageInCrisisScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("the fresh counter is there to be proliferated, alongside other counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Courage in Crisis")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, 1)

            game.castSpell(1, "Courage in Crisis", bears).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(bears, giant)).error shouldBe null
            game.resolveStack()

            plusOnes(game, bears) shouldBe 2
            plusOnes(game, giant) shouldBe 2
            game.state.projectedState.getPower(bears) shouldBe 4
            game.isInGraveyard(1, "Courage in Crisis") shouldBe true
        }
    }
}
