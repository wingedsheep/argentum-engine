package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Oran-Rief Ooze (ZNR #198) — {2}{G} Creature — Ooze, 2/2.
 *
 *   When this creature enters, put a +1/+1 counter on target creature you control.
 *   Whenever this creature attacks, put a +1/+1 counter on each attacking creature with a
 *   +1/+1 counter on it.
 */
class OranRiefOozeScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.giveCounter(id: EntityId) {
        state = state.updateEntity(id) {
            it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        }
    }

    init {
        test("enter trigger puts a +1/+1 counter on target creature you control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Oran-Rief Ooze")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Oran-Rief Ooze").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(bears)).error shouldBe null
            }
            game.resolveStack()

            withClue("the targeted Bears gets one +1/+1 counter") { game.plusOne(bears) shouldBe 1 }
        }

        test("attack trigger grows only attacking creatures that already have a +1/+1 counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Oran-Rief Ooze")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Raging Goblin")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val ooze = game.findPermanent("Oran-Rief Ooze")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val goblin = game.findPermanent("Raging Goblin")!!
            game.giveCounter(bears)
            game.giveCounter(goblin)

            game.declareAttackers(
                mapOf("Oran-Rief Ooze" to 2, "Grizzly Bears" to 2, "Hill Giant" to 2)
            ).error shouldBe null
            game.resolveStack()

            withClue("attacking Bears with a counter gets another") { game.plusOne(bears) shouldBe 2 }
            withClue("attacking Hill Giant without a counter gets none") { game.plusOne(giant) shouldBe 0 }
            withClue("the Ooze has no counter, so it gets none") { game.plusOne(ooze) shouldBe 0 }
            withClue("non-attacking Goblin with a counter gets none") { game.plusOne(goblin) shouldBe 1 }
        }
    }
}
