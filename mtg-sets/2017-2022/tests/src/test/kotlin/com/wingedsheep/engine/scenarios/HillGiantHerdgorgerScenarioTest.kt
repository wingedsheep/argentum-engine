package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hill Giant Herdgorger (AFR #187) — "When this creature enters, you gain 3 life."
 */
class HillGiantHerdgorgerScenarioTest : ScenarioTestBase() {

    init {
        test("entering gains its controller 3 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hill Giant Herdgorger")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Hill Giant Herdgorger").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Hill Giant Herdgorger") shouldBe true
            game.getLifeTotal(1) shouldBe 23
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
