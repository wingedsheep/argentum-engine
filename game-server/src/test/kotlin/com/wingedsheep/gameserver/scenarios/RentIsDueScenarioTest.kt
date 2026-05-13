package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rent Is Due.
 *
 * Card reference:
 * - Rent Is Due ({W}): Enchantment
 *   At the beginning of your upkeep, tap two untapped creatures and/or Treasures you control.
 *   If you do, draw a card. Otherwise, sacrifice Rent Is Due.
 */
class RentIsDueScenarioTest : ScenarioTestBase() {

    init {
        context("Rent Is Due — cast and enter the battlefield") {

            test("resolves and enters battlefield as an Enchantment when cast for {W}") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Rent Is Due")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Rent Is Due")
                withClue("Casting Rent Is Due for {W} should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Rent Is Due should be on the battlefield") {
                    game.isOnBattlefield("Rent Is Due") shouldBe true
                }
                withClue("Rent Is Due should no longer be in the caster's hand") {
                    game.isInHand(1, "Rent Is Due") shouldBe false
                }
            }
        }
    }
}
