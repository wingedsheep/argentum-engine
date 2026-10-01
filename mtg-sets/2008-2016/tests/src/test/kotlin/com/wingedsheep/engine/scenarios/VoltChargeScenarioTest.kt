package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Volt Charge (NPH #100, reprinted in DDL and ONE) — {2}{R} Instant.
 *
 *   Volt Charge deals 3 damage to any target. Proliferate.
 */
class VoltChargeScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Volt Charge")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("deals 3 damage to a player, then proliferates a chosen permanent") {
            val game = board()
            val mine = game.findPermanent("Hill Giant")!!
            seed(game, mine, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpellTargetingPlayer(1, "Volt Charge", 2).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(mine)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            count(game, mine, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("deals 3 damage to a creature, killing it; proliferate with nothing to choose doesn't prompt") {
            val game = board()
            val theirs = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Volt Charge", theirs).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Volt Charge") shouldBe true
        }
    }
}
