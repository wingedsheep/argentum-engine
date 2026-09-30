package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Drown in Ichor (ONE #91) — {1}{B} Sorcery.
 *
 *   Target creature gets -4/-4 until end of turn. Proliferate.
 *
 * Pins the -4/-4 killing a small creature while proliferate grows another permanent's counters,
 * and that proliferating onto the target itself happens before state-based actions, saving it.
 */
class DrownInIchorScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Drown in Ichor")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("kills the target and proliferates another permanent's counters") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Drown in Ichor", bears).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getPower(giant) shouldBe 5
        }

        test("proliferating onto the target happens before state-based actions") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            // 3/3 with one +1/+1 counter = 4/4; -4/-4 alone would leave 0/0, proliferate makes it 1/1.
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Drown in Ichor", giant).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getToughness(giant) shouldBe 1
        }

        test("choosing nothing to proliferate still applies -4/-4") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 2)

            game.castSpell(1, "Drown in Ichor", giant).error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.state.projectedState.getToughness(giant) shouldBe 1
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }
    }
}
