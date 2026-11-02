package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Waterknot (RIX #61) — taps the enchanted creature on entering, and it doesn't untap during its
 * controller's untap step.
 */
class WaterknotScenarioTest : ScenarioTestBase() {
    init {
        test("taps the enchanted creature, which stays tapped through its controller's untap step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Waterknot")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Waterknot", giant).error shouldBe null
            game.resolveStack()
            game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
        }
    }
}
