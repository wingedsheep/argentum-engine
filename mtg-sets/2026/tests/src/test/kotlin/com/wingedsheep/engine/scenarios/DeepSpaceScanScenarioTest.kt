package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deep Space Scan: draw one card per differently named land you control, then no maximum hand
 * size until your next turn — so this turn's cleanup discards nothing, but the limit is back once
 * your next turn begins.
 */
class DeepSpaceScanScenarioTest : ScenarioTestBase() {

    init {
        context("Deep Space Scan") {
            test("draws per differently named land and skips this turn's discard to hand size") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deep Space Scan")
                    .withCardsInHand(1, "Grizzly Bears", 6)
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .apply { repeat(10) { withCardInLibrary(1, "Plains") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Deep Space Scan").error shouldBe null
                game.resolveStack()

                withClue("three differently named lands (Island, Forest, Mountain) draw three") {
                    game.handSize(1) shouldBe 9
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("no discard at this turn's cleanup") { game.handSize(1) shouldBe 9 }
            }
        }
    }
}
