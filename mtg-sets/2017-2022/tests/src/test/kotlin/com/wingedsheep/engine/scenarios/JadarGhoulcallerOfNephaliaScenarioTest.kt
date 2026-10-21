package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Jadar, Ghoulcaller of Nephalia (MID #108).
 *
 * "At the beginning of your end step, if you control no creatures with decayed, create a 2/2
 *  black Zombie creature token with decayed."
 */
class JadarGhoulcallerOfNephaliaScenarioTest : ScenarioTestBase() {

    init {
        context("Jadar, Ghoulcaller of Nephalia") {

            test("creates a decayed Zombie at your end step when you control no creatures with decayed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jadar, Ghoulcaller of Nephalia")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val tokens = game.findPermanents("Zombie Token")
                tokens shouldHaveSize 1
                game.state.projectedState.hasKeyword(tokens.single(), Keyword.DECAYED) shouldBe true
            }

            test("does nothing while you control a creature with decayed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jadar, Ghoulcaller of Nephalia")
                    .withCardOnBattlefield(1, "Rot-Curse Rakshasa")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.findPermanents("Zombie Token").shouldBeEmpty()
            }
        }
    }
}
