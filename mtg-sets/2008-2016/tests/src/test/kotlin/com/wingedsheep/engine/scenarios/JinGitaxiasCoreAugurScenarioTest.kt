package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jin-Gitaxias, Core Augur — {8}{U}{U} Legendary Creature — Phyrexian Praetor 5/4.
 *   Flash
 *   At the beginning of your end step, draw seven cards.
 *   Each opponent's maximum hand size is reduced by seven.
 */
class JinGitaxiasCoreAugurScenarioTest : ScenarioTestBase() {

    private fun TestGame.maxHandSize(playerNumber: Int): Int? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return getClientState(1).players.first { it.playerId == playerId }.maxHandSize
    }

    init {
        context("Jin-Gitaxias, Core Augur") {
            test("each opponent's maximum hand size drops to zero; its controller's stays seven") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jin-Gitaxias, Core Augur")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.maxHandSize(1) shouldBe 7
                game.maxHandSize(2) shouldBe 0
            }

            test("draws seven at its controller's end step") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jin-Gitaxias, Core Augur")
                repeat(10) { builder.withCardInLibrary(1, "Island") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.handSize(1) shouldBe 7
            }

            test("the opponent discards their whole hand at their own cleanup") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jin-Gitaxias, Core Augur")
                    .withCardsInHand(2, "Island", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("maximum hand size zero: all three discarded") { game.handSize(2) shouldBe 0 }
                game.graveyardSize(2) shouldBe 3
            }

            test("an opponent with no maximum hand size has nothing to reduce") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Reliquary Tower")
                    .withCardInHand(1, "Jin-Gitaxias, Core Augur")
                    .withLandsOnBattlefield(1, "Island", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Jin-Gitaxias, Core Augur").error shouldBe null
                game.resolveStack()
                game.maxHandSize(2) shouldBe null
            }
        }
    }
}
