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
 * Village Cannibals (ISD #125) — {2}{B} 2/2 Human.
 *
 *   Whenever another Human creature dies, put a +1/+1 counter on this creature.
 */
class VillageCannibalsScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("grows only when another Human dies") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Village Cannibals")
                .withCardOnBattlefield(2, "Elite Vanguard")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardsInHand(1, "Lightning Bolt", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cannibals = game.findPermanent("Village Cannibals")!!

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            withClue("Grizzly Bears isn't a Human") { game.plusOnes(cannibals) shouldBe 0 }

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Elite Vanguard")!!).error shouldBe null
            game.resolveStack()
            withClue("Elite Vanguard is a Human") { game.plusOnes(cannibals) shouldBe 1 }
            game.state.projectedState.getPower(cannibals) shouldBe 3
        }
    }
}
