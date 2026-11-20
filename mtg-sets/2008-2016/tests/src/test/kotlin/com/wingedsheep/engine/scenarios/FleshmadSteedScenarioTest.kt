package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fleshmad Steed (THS #88) — {1}{B} Creature — Horse 2/2.
 * "Whenever another creature dies, tap this creature."
 */
class FleshmadSteedScenarioTest : ScenarioTestBase() {
    init {
        test("another creature dying taps the Steed") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Fleshmad Steed")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val steed = game.findPermanent("Fleshmad Steed")!!

            game.state.getEntity(steed)?.has<TappedComponent>() shouldBe false

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            withClue("the Bears died") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
            withClue("the Steed tapped itself") {
                game.state.getEntity(steed)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
