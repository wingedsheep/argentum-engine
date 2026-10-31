package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Pledge of Unity (WAR #210) — {1}{G}{W} Instant.
 *
 *   Put a +1/+1 counter on each creature you control. You gain 1 life for each creature you control.
 */
class PledgeOfUnityScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("counters each of your creatures and gains 1 life per creature you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pledge of Unity")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Pledge of Unity").error shouldBe null
            game.resolveStack()

            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 1
            game.plusOnes(game.findPermanent("Glory Seeker")!!) shouldBe 1
            game.plusOnes(game.findPermanent("Hill Giant")!!) shouldBe 0
            game.getLifeTotal(1) shouldBe 22
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
