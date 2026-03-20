package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Tests for Dawn's Truce.
 *
 * Dawn's Truce
 * {1}{W}
 * Instant
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 *
 * You and permanents you control gain hexproof until end of turn.
 * If the gift was promised, permanents you control also gain indestructible
 * until end of turn.
 */
class DawnsTruceScenarioTest : ScenarioTestBase() {

    init {
        test("Dawn's Truce without gift grants hexproof to player") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dawn's Truce")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val p2HandBefore = game.handSize(2)

            // Cast Dawn's Truce
            game.castSpell(1, "Dawn's Truce")
            game.resolveStack()

            // Decline the gift (say no to optional cost)
            game.answerYesNo(false)

            // Player 2 should NOT have drawn a card
            game.handSize(2) shouldBe p2HandBefore

            // Player 1 has hexproof — opponent cannot target them with Shock
            game.passPriority() // P1 passes priority to P2
            val result = game.castSpellTargetingPlayer(2, "Shock", 1)
            result.isSuccess shouldBe false
        }

        test("Dawn's Truce with gift lets opponent draw and grants hexproof") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dawn's Truce")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val p2HandBefore = game.handSize(2)

            // Cast Dawn's Truce
            game.castSpell(1, "Dawn's Truce")
            game.resolveStack()

            // Accept the gift (say yes to optional cost)
            game.answerYesNo(true)

            // Player 2 should have drawn a card from the gift
            game.handSize(2) shouldBe p2HandBefore + 1

            // Player 1 has hexproof — opponent cannot target them
            game.passPriority()
            val result = game.castSpellTargetingPlayer(2, "Shock", 1)
            result.isSuccess shouldBe false
        }

        test("Dawn's Truce hexproof expires at end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dawn's Truce")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            // Cast Dawn's Truce, decline gift
            game.castSpell(1, "Dawn's Truce")
            game.resolveStack()
            game.answerYesNo(false)

            // Advance to next turn
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            // Now Player 1's hexproof should have expired
            val result = game.castSpellTargetingPlayer(2, "Shock", 1)
            result.isSuccess shouldBe true
        }
    }
}
