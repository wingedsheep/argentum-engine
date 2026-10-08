package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Powerstone Engineer (BRO #20) — {1}{W} 2/1. "When this creature dies, create a tapped
 * Powerstone token."
 */
class PowerstoneEngineerScenarioTest : ScenarioTestBase() {

    init {
        test("dying creates a tapped Powerstone artifact token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Powerstone Engineer")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shock", game.findPermanent("Powerstone Engineer")!!).error shouldBe null
            game.resolveStack()

            game.findPermanent("Powerstone Engineer") shouldBe null
            val powerstone = game.findPermanent("Powerstone")
            powerstone shouldNotBe null
            val entity = game.state.getEntity(powerstone!!)!!
            entity.has<TokenComponent>() shouldBe true
            entity.has<TappedComponent>() shouldBe true
            game.state.projectedState.hasType(powerstone, "ARTIFACT") shouldBe true
            game.state.projectedState.getController(powerstone) shouldBe game.player1Id
        }
    }
}
