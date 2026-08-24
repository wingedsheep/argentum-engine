package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cleansing — "For each land, destroy that land unless any player pays 1 life."
 *
 * The two things worth proving are that the ransom is *per land* rather than one board-wide
 * question, and that it is symmetrical: the caster's own lands are on the block too. With every
 * offer declined, every land on both sides goes.
 */
class CleansingScenarioTest : ScenarioTestBase() {

    init {
        context("Cleansing") {

            test("with nobody paying, every land on both sides is destroyed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cleansing")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Cleansing").error shouldBe null
                game.resolveStack()
                // Every per-land offer is declined by both players.
                var guard = 0
                while (game.hasPendingDecision() && guard++ < 60) {
                    game.answerYesNo(false)
                }

                withClue("the caster's own lands are not spared") {
                    game.findPermanents("Plains").size shouldBe 0
                }
                withClue("nor the opponent's") {
                    game.findPermanents("Swamp").size shouldBe 0
                }
            }
        }
    }
}
