package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Mowu, Loyal Companion — "If one or more +1/+1 counters would be put on Mowu, that many plus one
 * +1/+1 counters are put on it instead." Self-scoped only, and whoever puts the counters.
 */
class MowuLoyalCompanionScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(active: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Mowu, Loyal Companion")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(active, "Battlegrowth")
        .withCardInHand(active, "Battlegrowth")
        .withLandsOnBattlefield(active, "Forest", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(active)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("a counter put on Mowu becomes two; another creature you control is untouched") {
            val game = board(active = 1).build()
            val mowu = game.findPermanent("Mowu, Loyal Companion")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Battlegrowth", targetId = mowu).error shouldBe null
            game.resolveStack()
            game.plusOnes(mowu) shouldBe 2

            game.castSpell(1, "Battlegrowth", targetId = bears).error shouldBe null
            game.resolveStack()
            game.plusOnes(bears) shouldBe 1
        }

        test("an opponent putting a counter on Mowu also adds one more") {
            val game = board(active = 2).build()
            val mowu = game.findPermanent("Mowu, Loyal Companion")!!

            game.castSpell(2, "Battlegrowth", targetId = mowu).error shouldBe null
            game.resolveStack()
            game.plusOnes(mowu) shouldBe 2
        }
    }
}
