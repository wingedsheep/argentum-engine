package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Lurking Roper (AFR #194) — "This creature doesn't untap during your untap step. / Whenever you
 * gain life, untap this creature."
 */
class LurkingRoperScenarioTest : ScenarioTestBase() {

    init {
        test("gaining life untaps it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lurking Roper", tapped = true)
                .withCardInHand(1, "Sacred Nectar")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val roper = game.findPermanent("Lurking Roper")!!
            game.state.getEntity(roper)!!.has<TappedComponent>() shouldBe true

            game.castSpell(1, "Sacred Nectar").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 24
            game.state.getEntity(roper)!!.has<TappedComponent>() shouldBe false
        }
    }
}
