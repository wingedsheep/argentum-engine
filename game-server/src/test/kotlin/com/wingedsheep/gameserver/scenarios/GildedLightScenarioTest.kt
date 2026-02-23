package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Tests for Gilded Light.
 *
 * Gilded Light
 * {1}{W}
 * Instant
 * You gain shroud until end of turn. (You can't be the target of spells or abilities.)
 * Cycling {2}
 */
class GildedLightScenarioTest : ScenarioTestBase() {

    init {
        test("Gilded Light grants player shroud until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gilded Light")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            // Cast Gilded Light
            game.castSpell(1, "Gilded Light")
            game.resolveStack()

            // Player 1 now has shroud — opponent cannot target them with Shock
            game.passPriority() // P1 passes priority to P2
            val result = game.castSpellTargetingPlayer(2, "Shock", 1)
            result.isSuccess shouldBe false
        }

        test("Gilded Light shroud expires at end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gilded Light")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            // Cast Gilded Light
            game.castSpell(1, "Gilded Light")
            game.resolveStack()

            // Advance to end step, then to next turn's main phase
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            // Now Player 1's shroud should have expired
            // P2 is active player, they can target Player 1
            val result = game.castSpellTargetingPlayer(2, "Shock", 1)
            result.isSuccess shouldBe true
        }

        test("Gilded Light can be cycled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gilded Light")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val handBefore = game.handSize(1)

            // Cycle Gilded Light (costs {2}, discards card, draws a card)
            val result = game.cycleCard(1, "Gilded Light")
            result.isSuccess shouldBe true

            // Hand size should stay the same (discard 1, draw 1)
            game.handSize(1) shouldBe handBefore

            // Gilded Light should be in graveyard
            game.isInGraveyard(1, "Gilded Light") shouldBe true
        }
    }
}
