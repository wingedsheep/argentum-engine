package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Balemurk Leech (DSK #84).
 *
 * Balemurk Leech — {1}{B} Creature — Leech, 2/2
 *   "Eerie — Whenever an enchantment you control enters and whenever you fully unlock a
 *    Room, each opponent loses 1 life."
 *
 * Verifies the enchantment-enters Eerie trigger drains each opponent for 1 life, and that an
 * opponent's enchantment entering does NOT trigger it (the trigger is "an enchantment you control").
 */
class BalemurkLeechScenarioTest : ScenarioTestBase() {

    init {
        context("Balemurk Leech — Eerie (enchantment enters)") {

            test("an enchantment you control entering drains each opponent for 1 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Balemurk Leech")
                    .withCardInHand(1, "Test Enchantment") // {1}{W}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentLifeBefore = game.getLifeTotal(2)

                val cast = game.castSpell(1, "Test Enchantment")
                withClue("Casting Test Enchantment should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Each opponent loses 1 life when an enchantment you control enters") {
                    game.getLifeTotal(2) shouldBe opponentLifeBefore - 1
                }
            }

            test("an opponent's enchantment entering does NOT trigger the Eerie drain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Balemurk Leech")
                    .withCardInHand(2, "Test Enchantment") // {1}{W}, controlled by the opponent
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val controllerLifeBefore = game.getLifeTotal(1)

                val cast = game.castSpell(2, "Test Enchantment")
                withClue("Opponent casting Test Enchantment should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The Leech's controller loses no life — the enchantment isn't theirs") {
                    game.getLifeTotal(1) shouldBe controllerLifeBefore
                }
            }
        }
    }
}
