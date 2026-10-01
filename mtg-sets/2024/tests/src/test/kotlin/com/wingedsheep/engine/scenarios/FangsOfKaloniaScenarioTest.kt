package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Fangs of Kalonia (MH3) — "Put a +1/+1 counter on target creature you control, then double the
 * number of +1/+1 counters on each creature that had a +1/+1 counter put on it this way.
 * Overload {4}{G}{G}".
 */
class FangsOfKaloniaScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.plusCounters(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun TestGame.addPlusCounters(id: EntityId, n: Int) {
            state = state.updateEntity(id) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, n))
            }
        }

        test("cast normally: the target gets a counter, then its counters double") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Fangs of Kalonia")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.addPlusCounters(bears, 1)

            game.castSpell(1, "Fangs of Kalonia", targetId = bears).error shouldBe null
            game.resolveStack()

            // 1 + 1 = 2, doubled to 4. The untargeted Hill Giant gets nothing.
            game.plusCounters(bears) shouldBe 4
            game.plusCounters(giant) shouldBe 0
        }

        test("overloaded: each creature you control gets a counter and has its counters doubled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Fangs of Kalonia")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val (myBears, theirBears) = game.findPermanents("Grizzly Bears")
                .partition { game.state.projectedState.getController(it) == game.player1Id }
                .let { (mine, theirs) -> mine.single() to theirs.single() }
            val giant = game.findPermanent("Hill Giant")!!
            game.addPlusCounters(giant, 2)

            game.castSpellWithOverload(1, "Fangs of Kalonia").error shouldBe null
            game.resolveStack()

            game.plusCounters(myBears) shouldBe 2 // 0 + 1, doubled
            game.plusCounters(giant) shouldBe 6 // 2 + 1, doubled
            game.plusCounters(theirBears) shouldBe 0
        }
    }
}
