package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mechanized Production (AER #38) — {2}{U}{U} Enchantment — Aura.
 *
 * "Enchant artifact you control. At the beginning of your upkeep, create a token that's a copy of
 * enchanted artifact. Then if you control eight or more artifacts with the same name as one
 * another, you win the game."
 *
 * The win check is the largest same-named group of artifacts, not the artifact count: nine
 * artifacts split five/four don't win, and the eight need not be copies of the enchanted one.
 */
class MechanizedProductionScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.withCopies(name: String, n: Int): ScenarioBuilder {
        var b = this
        repeat(n) { b = b.withCardOnBattlefield(1, name) }
        return b
    }

    private fun TestGame.passToAliceUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    init {
        context("Mechanized Production") {

            test("the eighth same-named artifact wins the game on resolution") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCopies("Ornithopter", 7)
                    .withCardAttachedTo(1, "Mechanized Production", "Ornithopter")
                    // Start on Bob's turn so the next upkeep reached is Alice's.
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state.gameOver shouldBe false
                game.passToAliceUpkeep()

                withClue("the token copy made eight Ornithopters, and Alice won") {
                    game.findPermanents("Ornithopter").size shouldBe 8
                    game.state.gameOver shouldBe true
                    game.state.winnerId shouldBe game.player1Id
                }
            }

            test("seven same-named artifacts only copy, no win") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCopies("Ornithopter", 6)
                    .withCardAttachedTo(1, "Mechanized Production", "Ornithopter")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passToAliceUpkeep()

                game.findPermanents("Ornithopter").size shouldBe 7
                game.state.gameOver shouldBe false
            }

            test("nine artifacts split five and four don't share a name eight times") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCopies("Ornithopter", 4)
                    .withCopies("Memnite", 4)
                    .withCardAttachedTo(1, "Mechanized Production", "Ornithopter")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passToAliceUpkeep()

                withClue("five Ornithopters and four Memnites — nine artifacts, largest group five") {
                    game.findPermanents("Ornithopter").size shouldBe 5
                    game.findPermanents("Memnite").size shouldBe 4
                    game.state.gameOver shouldBe false
                }
            }

            test("the eight need not share the enchanted artifact's name") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCopies("Ornithopter", 8)
                    .withCardOnBattlefield(1, "Millstone")
                    .withCardAttachedTo(1, "Mechanized Production", "Millstone")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("controlling eight already doesn't win until the trigger resolves") {
                    game.state.gameOver shouldBe false
                }
                game.passToAliceUpkeep()

                withClue("a second Millstone, but eight Ornithopters win it") {
                    game.findPermanents("Millstone").size shouldBe 2
                    game.state.winnerId shouldBe game.player1Id
                }
            }
        }
    }
}
