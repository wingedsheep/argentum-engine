package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gift of the Viper (MH3) — "Put a +1/+1 counter, a reach counter, and a deathtouch counter on
 * target creature. Untap it."
 */
class GiftOfTheViperScenarioTest : ScenarioTestBase() {

    init {
        test("target gets all three counters, gains reach and deathtouch, and untaps") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gift of the Viper")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Gift of the Viper", targetId = bears).error shouldBe null
            game.resolveStack()

            val counters = game.state.getEntity(bears)!!.get<CountersComponent>()!!
            counters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            counters.getCount(CounterType.REACH) shouldBe 1
            counters.getCount(CounterType.DEATHTOUCH) shouldBe 1

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.REACH) shouldBe true
            projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
            (game.state.getEntity(bears)!!.get<TappedComponent>() == null) shouldBe true
        }

        test("can target an opponent's creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gift of the Viper")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Gift of the Viper", targetId = bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
            (game.state.getEntity(bears)!!.get<TappedComponent>() == null) shouldBe true
        }
    }
}
