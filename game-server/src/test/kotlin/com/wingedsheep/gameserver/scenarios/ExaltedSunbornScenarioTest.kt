package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Exalted Sunborn.
 *
 * Card reference:
 * - Exalted Sunborn ({3}{W}{W}): 4/5 Creature — Angel Wizard
 *   "Flying, lifelink
 *    If one or more tokens would be created under your control, twice that many
 *    of those tokens are created instead.
 *    Warp {1}{W}"
 *
 * Exercises the [DoubleTokenCreation] replacement effect — the first card in the
 * codebase to use it. Uses Hop to It (creates three 1/1 Rabbit tokens) as the
 * token-creating spell.
 */
class ExaltedSunbornScenarioTest : ScenarioTestBase() {

    init {
        context("Exalted Sunborn — token doubling") {

            test("baseline: Hop to It creates 3 Rabbit tokens without Exalted Sunborn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hop to It")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpell(1, "Hop to It")
                withClue("Hop to It cast should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Baseline: 3 Rabbit tokens enter") {
                    game.findAllPermanents("Rabbit Token").size shouldBe 3
                }
            }

            test("one Exalted Sunborn doubles Hop to It from 3 to 6 tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Exalted Sunborn")
                    .withCardInHand(1, "Hop to It")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpell(1, "Hop to It")
                withClue("Hop to It cast should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("3 tokens × 2 doubling = 6 Rabbit tokens") {
                    game.findAllPermanents("Rabbit Token").size shouldBe 6
                }
            }

            test("two Exalted Sunborns multiply Hop to It from 3 to 12 tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Exalted Sunborn")
                    .withCardOnBattlefield(1, "Exalted Sunborn")
                    .withCardInHand(1, "Hop to It")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpell(1, "Hop to It")
                withClue("Hop to It cast should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("3 tokens × 2 × 2 = 12 Rabbit tokens (two doublers stack)") {
                    game.findAllPermanents("Rabbit Token").size shouldBe 12
                }
            }

            test("opponent's Exalted Sunborn does not double your tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Exalted Sunborn") // opponent's Sunborn
                    .withCardInHand(1, "Hop to It")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpell(1, "Hop to It")
                withClue("Hop to It cast should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Doubling only applies for the player who controls Exalted Sunborn — still 3 tokens") {
                    game.findAllPermanents("Rabbit Token").size shouldBe 3
                }
            }
        }
    }
}
