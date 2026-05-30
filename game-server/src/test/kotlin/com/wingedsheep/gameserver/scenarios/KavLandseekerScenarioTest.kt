package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kav Landseeker, which exercises the
 * [com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming.NEXT_TURN] timing on
 * [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect].
 *
 * Oracle: "When this creature enters, create a Lander token. At the beginning of the end
 * step on your next turn, sacrifice that token."
 *
 * The "on your next turn" timing must NOT fire on the same turn Kav enters, even if Kav
 * resolves before the END step. It must also skip past the opponent's intervening end
 * step and land on the controller's following own end step.
 */
class KavLandseekerScenarioTest : ScenarioTestBase() {

    init {
        test("Lander is NOT sacrificed at end of the turn Kav Landseeker enters") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Kav Landseeker")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            game.castSpell(1, "Kav Landseeker")
            game.resolveStack()

            withClue("Lander should be on the battlefield after Kav's ETB") {
                game.isOnBattlefield("Lander") shouldBe true
            }

            // Walk to end step of the SAME turn. The delayed trigger must not fire.
            game.passUntilPhase(Phase.ENDING, Step.END)
            if (game.state.stack.isNotEmpty()) {
                game.resolveStack()
            }

            withClue("Lander must still be on the battlefield at end of current turn") {
                game.isOnBattlefield("Lander") shouldBe true
            }
        }

        test("Lander survives the opponent's end step and is sacrificed at controller's next end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Kav Landseeker")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            game.castSpell(1, "Kav Landseeker")
            game.resolveStack()

            withClue("Lander should be on the battlefield after Kav's ETB") {
                game.isOnBattlefield("Lander") shouldBe true
            }

            // End of Player1's turn — Lander must survive.
            game.passUntilPhase(Phase.ENDING, Step.END)
            if (game.state.stack.isNotEmpty()) game.resolveStack()
            withClue("Lander survives Player1's end step (same turn it was created)") {
                game.isOnBattlefield("Lander") shouldBe true
            }

            // Advance into Player2's turn and to their end step. fireOnlyOnControllersTurn
            // must keep the trigger from firing for Player1 on Player2's end step.
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.ENDING, Step.END)
            if (game.state.stack.isNotEmpty()) game.resolveStack()
            withClue("Lander survives Player2's end step") {
                game.isOnBattlefield("Lander") shouldBe true
            }

            // Player1's next turn — end step should now sacrifice the Lander.
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.ENDING, Step.END)
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("Lander should be sacrificed at Player1's next end step") {
                game.isOnBattlefield("Lander") shouldBe false
            }
        }
    }
}
