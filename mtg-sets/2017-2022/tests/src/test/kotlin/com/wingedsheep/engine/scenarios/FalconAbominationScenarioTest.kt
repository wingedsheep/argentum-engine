package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Falcon Abomination (MID #52).
 *
 * "Flying. When this creature enters, create a 2/2 black Zombie creature token with decayed."
 */
class FalconAbominationScenarioTest : ScenarioTestBase() {

    init {
        context("Falcon Abomination") {

            test("entering creates a 2/2 Zombie token that has decayed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Falcon Abomination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Falcon Abomination").error shouldBe null
                game.resolveStack()

                val tokens = game.findPermanents("Zombie Token")
                tokens shouldHaveSize 1
                val token = tokens.single()
                game.state.projectedState.hasKeyword(token, Keyword.DECAYED) shouldBe true
                game.state.projectedState.getPower(token) shouldBe 2
                game.state.projectedState.getToughness(token) shouldBe 2
            }
        }
    }
}
