package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hellkite Tyrant (GTC #94) — {4}{R}{R} Creature — Dragon 6/5.
 *
 * "Whenever this creature deals combat damage to a player, gain control of all artifacts that
 * player controls. At the beginning of your upkeep, if you control twenty or more artifacts, you
 * win the game."
 *
 * The steal reads the damaged player through the trigger context and takes every artifact they
 * control — not their lands. The win is an intervening-if on the artifact count.
 */
class HellkiteTyrantScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.withCopies(player: Int, name: String, n: Int): ScenarioBuilder {
        var b = this
        repeat(n) { b = b.withCardOnBattlefield(player, name) }
        return b
    }

    private fun TestGame.passToAliceUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    init {
        test("combat damage to a player steals all the artifacts that player controls") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Hellkite Tyrant", summoningSickness = false)
                .withCopies(2, "Ornithopter", 2)
                .withCardOnBattlefield(2, "Millstone")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Hellkite Tyrant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            if (game.state.pendingDecision != null) game.submitDefaultCombatDamage()
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20 - 6
            val projected = game.state.projectedState
            withClue("every artifact Bob controlled is now Alice's") {
                (game.findAllPermanents("Ornithopter") + game.findAllPermanents("Millstone")).forEach {
                    projected.getController(it) shouldBe game.player1Id
                }
            }
            withClue("Bob's lands are not artifacts and stay with him") {
                game.findAllPermanents("Mountain").forEach { projected.getController(it) shouldBe game.player2Id }
            }
        }

        test("twenty artifacts at upkeep wins the game") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Hellkite Tyrant")
                .withCopies(1, "Ornithopter", 20)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.state.gameOver shouldBe false
            game.passToAliceUpkeep()

            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe game.player1Id
        }

        test("nineteen artifacts at upkeep does nothing") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Hellkite Tyrant")
                .withCopies(1, "Ornithopter", 19)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passToAliceUpkeep()

            game.state.gameOver shouldBe false
        }
    }
}
