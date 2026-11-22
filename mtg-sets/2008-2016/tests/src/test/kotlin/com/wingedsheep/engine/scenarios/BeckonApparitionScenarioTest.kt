package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Beckon Apparition (EVE) — exile target card from a graveyard, then create a 1/1 white and black
 * Spirit token with flying.
 */
class BeckonApparitionScenarioTest : ScenarioTestBase() {
    init {
        test("exiles a card from an opponent's graveyard and makes a flying Spirit") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInHand(1, "Beckon Apparition")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingGraveyardCard(1, "Beckon Apparition", 2, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            val spirit = game.findPermanent("Spirit Token")!!
            game.state.projectedState.getPower(spirit) shouldBe 1
            game.state.projectedState.getToughness(spirit) shouldBe 1
            game.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
        }
    }
}
