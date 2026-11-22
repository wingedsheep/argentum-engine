package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/** Knight Watch (GTC) — create two 2/2 white Knight creature tokens with vigilance. */
class KnightWatchScenarioTest : ScenarioTestBase() {
    init {
        test("creates two 2/2 vigilant Knight tokens") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Knight Watch")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Knight Watch").error shouldBe null
            game.resolveStack()

            val knights = game.findPermanents("Knight Token")
            knights shouldHaveSize 2
            val projected = game.state.projectedState
            knights.forEach { knight ->
                projected.getPower(knight) shouldBe 2
                projected.getToughness(knight) shouldBe 2
                projected.hasKeyword(knight, Keyword.VIGILANCE) shouldBe true
            }
        }
    }
}
