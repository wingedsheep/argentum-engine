package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Strength of the Pack (RIX #145) — "Put two +1/+1 counters on each creature you control."
 */
class StrengthOfThePackScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("each creature you control gets two +1/+1 counters, opponents' don't") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Strength of the Pack")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Ravenous Daggertooth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Strength of the Pack").error shouldBe null
            game.resolveStack()

            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 2
            game.plusOnes(game.findPermanent("Llanowar Elves")!!) shouldBe 2
            game.plusOnes(game.findPermanent("Ravenous Daggertooth")!!) shouldBe 0
        }
    }
}
