package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Rot Shambler — grows when another creature you control dies, not when an opponent's does. */
class RotShamblerScenarioTest : ScenarioTestBase() {

    private fun TestGame.shamblerCounters(): Int =
        state.getEntity(findPermanent("Rot Shambler")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Rot Shambler")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("another creature you control dying adds a +1/+1 counter") {
            val game = board()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.shamblerCounters() shouldBe 1
        }

        test("an opponent's creature dying doesn't") {
            val game = board()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Savannah Lions")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Savannah Lions") shouldBe true
            game.shamblerCounters() shouldBe 0
        }
    }
}
