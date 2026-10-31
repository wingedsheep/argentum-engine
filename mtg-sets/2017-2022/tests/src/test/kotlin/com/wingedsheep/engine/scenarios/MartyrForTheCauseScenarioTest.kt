package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Martyr for the Cause (WAR #23) — {1}{W} Creature — Human Soldier 2/2.
 *
 *   When this creature dies, proliferate.
 */
class MartyrForTheCauseScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("dying proliferates") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Martyr for the Cause")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, 1)

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Martyr for the Cause")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Martyr for the Cause") shouldBe true

            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            plusOnes(game, giant) shouldBe 2
        }
    }
}
