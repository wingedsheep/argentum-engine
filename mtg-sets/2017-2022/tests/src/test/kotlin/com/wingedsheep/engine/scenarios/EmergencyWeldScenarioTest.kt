package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Emergency Weld (BRO #93) — return target artifact or creature card from your graveyard to your
 * hand, then create a 1/1 colorless Soldier artifact creature token.
 */
class EmergencyWeldScenarioTest : ScenarioTestBase() {

    init {
        test("returns an artifact card from the graveyard and creates a Soldier token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Emergency Weld")
                .withCardInGraveyard(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val target = game.findCardsInGraveyard(1, "Ornithopter")
            game.castSpellTargetingGraveyardCard(1, "Emergency Weld", target).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Ornithopter") shouldBe true
            game.findPermanents("Soldier Token").size shouldBe 1
        }
    }
}
