package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * No Witnesses (MKM #27) — {2}{W}{W} sorcery, "Each player who controls the most creatures
 * investigates. Then destroy all creatures."
 *
 * Covers the new `Conditions.PlayerControlsMostPermanents` on all three shapes the wording has: one
 * player strictly ahead (only they investigate), a tie (everyone tied investigates), and the case
 * where the caster is the one behind. Clue counts are read *after* the wipe, since the printed
 * order investigates first and destroys second.
 */
class NoWitnessesScenarioTest : ScenarioTestBase() {

    /** Board with [mine] / [theirs] Grizzly Bears and the sorcery in player 1's hand. */
    private fun board(mine: Int, theirs: Int): ScenarioBuilder {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "No Witnesses")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(mine) { builder = builder.withCardOnBattlefield(1, "Grizzly Bears") }
        repeat(theirs) { builder = builder.withCardOnBattlefield(2, "Grizzly Bears") }
        repeat(5) {
            builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
        }
        return builder
    }

    private fun TestGame.cluesOf(playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return findPermanents("Clue").count { state.projectedState.getController(it) == playerId }
    }

    init {
        test("only the player with the most creatures investigates, and everything dies") {
            val game = board(mine = 3, theirs = 1).build()

            game.castSpell(1, "No Witnesses").error shouldBe null
            game.resolveStack()

            withClue("player 1 was strictly ahead → exactly one Clue, and it's theirs") {
                game.cluesOf(1) shouldBe 1
                game.cluesOf(2) shouldBe 0
            }
            withClue("then destroy all creatures") {
                game.findPermanents("Grizzly Bears") shouldBe emptyList()
            }
        }

        test("the opponent investigates when they're the one ahead") {
            val game = board(mine = 1, theirs = 3).build()

            game.castSpell(1, "No Witnesses").error shouldBe null
            game.resolveStack()

            withClue("the Clue follows the board, not the caster") {
                game.cluesOf(1) shouldBe 0
                game.cluesOf(2) shouldBe 1
            }
            game.findPermanents("Grizzly Bears") shouldBe emptyList()
        }

        test("a tie means every tied player investigates") {
            val game = board(mine = 2, theirs = 2).build()

            game.castSpell(1, "No Witnesses").error shouldBe null
            game.resolveStack()

            withClue("'the most, or tied for the most' — both players qualify") {
                game.cluesOf(1) shouldBe 1
                game.cluesOf(2) shouldBe 1
            }
            game.findPermanents("Grizzly Bears") shouldBe emptyList()
        }

        test("an empty board is a tie at zero — everyone investigates") {
            val game = board(mine = 0, theirs = 0).build()

            game.castSpell(1, "No Witnesses").error shouldBe null
            game.resolveStack()

            withClue("nobody controls more creatures than anybody else") {
                game.cluesOf(1) shouldBe 1
                game.cluesOf(2) shouldBe 1
            }
        }
    }
}
