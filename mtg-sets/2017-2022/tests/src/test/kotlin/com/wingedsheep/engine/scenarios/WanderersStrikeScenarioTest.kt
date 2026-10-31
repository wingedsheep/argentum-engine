package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Wanderer's Strike (WAR #38) — {4}{W} Sorcery.
 *
 *   Exile target creature, then proliferate.
 */
class WanderersStrikeScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("exiles the target creature, then proliferates") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Wanderer's Strike")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, 1)

            game.castSpell(1, "Wanderer's Strike", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInExile(2, "Grizzly Bears") shouldBe true

            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            plusOnes(game, giant) shouldBe 2
            game.isInGraveyard(1, "Wanderer's Strike") shouldBe true
        }
    }
}
