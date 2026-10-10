package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Farmer Cotton: the cast-time X flows into the enters trigger and drives both token counts.
 */
class FarmerCottonScenarioTest : ScenarioTestBase() {
    init {
        fun castWithX(x: Int): TestGame {
            val game = scenario().withPlayers("Cotton", "Opponent")
                .withCardInHand(1, "Farmer Cotton")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castXSpell(1, "Farmer Cotton", x).error shouldBe null
            game.resolveStack()
            return game
        }

        test("X = 3 creates three Halflings and three Food") {
            val game = castWithX(3)
            game.isOnBattlefield("Farmer Cotton") shouldBe true
            game.findPermanents("Halfling Token").size shouldBe 3
            game.findPermanents("Food").size shouldBe 3
        }

        test("X = 0 creates nothing") {
            val game = castWithX(0)
            game.isOnBattlefield("Farmer Cotton") shouldBe true
            game.findPermanents("Halfling Token").size shouldBe 0
            game.findPermanents("Food").size shouldBe 0
        }
    }
}
