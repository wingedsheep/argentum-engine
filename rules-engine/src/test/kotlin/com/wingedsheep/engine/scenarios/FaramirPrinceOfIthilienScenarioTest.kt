package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Faramir, Prince of Ithilien.
 *
 * Oracle: "At the beginning of your end step, choose an opponent. At the beginning of
 * that player's next end step, you draw a card if they didn't attack you that turn.
 * Otherwise, create three 1/1 white Human Soldier creature tokens."
 *
 * Exercises:
 *  - the choose-an-opponent end-step trigger scheduling a delayed trigger keyed to the
 *    chosen opponent's next end step (new [CreateDelayedTriggerEffect.fireOnPlayer] usage
 *    driven by a targeted opponent), and
 *  - the new `PlayerAttackedPlayerThisTurn` condition (CR 508.6) reading the per-player
 *    attacked-players record stamped at declare-attackers time: didn't attack → draw,
 *    did attack → three Human Soldier tokens.
 */
class FaramirPrinceOfIthilienScenarioTest : ScenarioTestBase() {

    init {
        context("Faramir, Prince of Ithilien") {

            test("opponent did NOT attack you: you draw a card at their next end step") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Faramir, Prince of Ithilien")
                    .withActivePlayer(1)
                repeat(10) {
                    builder.withCardInLibrary(1, "Plains")
                    builder.withCardInLibrary(2, "Island")
                }
                val game = builder.build()

                // P1's end step: Faramir's trigger fires, auto-chooses the lone opponent (P2),
                // and schedules a delayed trigger at P2's next end step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Faramir scheduled a delayed trigger gated to P2's end step") {
                    game.state.delayedTriggers.size shouldBe 1
                    game.state.delayedTriggers[0].fireAtStep shouldBe Step.END
                    game.state.delayedTriggers[0].fireOnPlayerId shouldBe game.player2Id
                }

                // Cross into P2's turn (the next BEGINNING). P2 takes no combat action.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("It is now P2's turn") {
                    game.state.activePlayerId shouldBe game.player2Id
                }
                // Now P2's end step: the delayed trigger fires. P2 didn't attack P1 → P1 draws.
                game.passUntilPhase(Phase.ENDING, Step.END)
                val handBefore = game.handSize(1)
                game.resolveStack()

                withClue("P2 never attacked P1, so the delayed trigger draws P1 a card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                withClue("No Human Soldier tokens are created in the draw branch") {
                    game.findAllPermanents("Human Soldier Token").size shouldBe 0
                }
                withClue("Delayed trigger is consumed") {
                    game.state.delayedTriggers.size shouldBe 0
                }
            }

            test("opponent DID attack you: you create three 1/1 white Human Soldier tokens instead") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Faramir, Prince of Ithilien")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                repeat(10) {
                    builder.withCardInLibrary(1, "Plains")
                    builder.withCardInLibrary(2, "Island")
                }
                val game = builder.build()

                // P1's end step schedules the delayed trigger keyed to P2's end step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.state.delayedTriggers.size shouldBe 1

                // P2's turn: advance to P2's declare-attackers and swing the Bears at P1.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("It is now P2's turn") {
                    game.state.activePlayerId shouldBe game.player2Id
                }
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null

                // Advance to P2's end step: P2 attacked P1 this turn → P1 makes three tokens.
                game.passUntilPhase(Phase.ENDING, Step.END)
                val handBefore = game.handSize(1)
                val tokensBefore = game.findAllPermanents("Human Soldier Token").size
                game.resolveStack()

                withClue("P2 attacked P1, so the otherwise branch creates three Human Soldier tokens") {
                    game.findAllPermanents("Human Soldier Token").size shouldBe tokensBefore + 3
                }
                withClue("No card is drawn in the token branch") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("Delayed trigger is consumed") {
                    game.state.delayedTriggers.size shouldBe 0
                }
            }
        }
    }
}
