package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.HandRevealedEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hedron Alignment (OGW #57) — {2}{U} Enchantment.
 *
 *   Hexproof. At the beginning of your upkeep, you may reveal your hand. If you do, you win the
 *   game if you own a card named Hedron Alignment in exile, in your hand, in your graveyard, and
 *   on the battlefield. {1}{U}: Scry 1.
 *
 * Proves the reveal is a real hand reveal gated by the may-choice, that the four-zone test needs
 * every zone (an opponent-owned exiled copy doesn't stand in for yours), and that declining the
 * reveal forfeits the win.
 */
class HedronAlignmentScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.withAlignmentsExceptExile(): ScenarioBuilder =
        withCardOnBattlefield(1, "Hedron Alignment")
            .withCardInHand(1, "Hedron Alignment")
            .withCardInGraveyard(1, "Hedron Alignment")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

    private fun TestGame.passToPlayer1UpkeepPrompt() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
        withClue("the upkeep trigger asks whether to reveal") { (state.pendingDecision != null) shouldBe true }
    }

    init {
        test("revealing with a Hedron Alignment in all four zones wins the game") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withAlignmentsExceptExile()
                .withCardInExile(1, "Hedron Alignment")
                .build()

            game.passToPlayer1UpkeepPrompt()
            val result = game.answerYesNo(true)

            withClue("the hand is actually revealed") {
                result.events.any { it is HandRevealedEvent && it.revealingPlayerId == game.player1Id } shouldBe true
            }
            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe game.player1Id
        }

        test("declining to reveal forfeits the win") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withAlignmentsExceptExile()
                .withCardInExile(1, "Hedron Alignment")
                .build()

            game.passToPlayer1UpkeepPrompt()
            val result = game.answerYesNo(false)

            result.events.any { it is HandRevealedEvent } shouldBe false
            game.state.gameOver shouldBe false
        }

        test("an exiled copy the opponent owns does not complete the set") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withAlignmentsExceptExile()
                .withCardInExile(2, "Hedron Alignment")
                .build()

            game.passToPlayer1UpkeepPrompt()
            val result = game.answerYesNo(true)

            withClue("the reveal still happens") {
                result.events.any { it is HandRevealedEvent && it.revealingPlayerId == game.player1Id } shouldBe true
            }
            game.state.gameOver shouldBe false
        }
    }
}
