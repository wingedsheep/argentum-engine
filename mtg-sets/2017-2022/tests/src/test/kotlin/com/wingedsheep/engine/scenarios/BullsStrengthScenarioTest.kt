package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Bull's Strength (AFR #174) — "Target creature gets +2/+2 and gains trample until end of turn.
 * Untap it."
 */
class BullsStrengthScenarioTest : ScenarioTestBase() {

    init {
        test("pumps the target, grants trample, and untaps it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardInHand(1, "Bull's Strength")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true

            game.castSpell(1, "Bull's Strength", bears).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
        }
    }
}
