package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Censor (AKH #46) — {1}{U} Instant.
 *
 *   Counter target spell unless its controller pays {1}.
 *   Cycling {U}
 */
class CensorScenarioTest : ScenarioTestBase() {

    init {
        context("Censor") {

            test("counters a spell whose controller can't pay {1}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Censor")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2) // exactly {1}{G}, nothing left to pay
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "Censor", "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("cycling {U} draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Censor")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Censor").error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Censor") shouldBe true
                game.isInHand(1, "Plains") shouldBe true
            }
        }
    }
}
