package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Molten Blast (ZNR #149) — {2}{R} Instant.
 *
 *   Choose one —
 *   • Molten Blast deals 2 damage to target creature or planeswalker.
 *   • Destroy target artifact.
 */
class MoltenBlastScenarioTest : ScenarioTestBase() {

    init {
        context("Molten Blast's modes") {

            fun game() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Molten Blast")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Ornithopter")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("mode one deals 2 damage to target creature") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellWithMode(1, "Molten Blast", 0, bears).error shouldBe null
                game.resolveStack()

                withClue("2 damage kills the 2/2 Bears") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("mode two destroys target artifact") {
                val game = game()
                val thopter = game.findPermanent("Ornithopter")!!

                game.castSpellWithMode(1, "Molten Blast", 1, thopter).error shouldBe null
                game.resolveStack()

                withClue("the Ornithopter is destroyed; the Bears are untouched") {
                    game.isInGraveyard(2, "Ornithopter") shouldBe true
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
            }

            test("mode two cannot target a non-artifact creature") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Grizzly Bears is not an artifact") {
                    game.castSpellWithMode(1, "Molten Blast", 1, bears).error shouldNotBe null
                }
            }
        }
    }
}
