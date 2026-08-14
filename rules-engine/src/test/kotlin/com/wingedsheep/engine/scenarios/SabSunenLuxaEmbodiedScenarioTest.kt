package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Sab-Sunen, Luxa Embodied.
 *
 * Oracle:
 * - Reach, trample, indestructible
 * - "Sab-Sunen can't attack or block unless it has an even number of counters on it. (Zero is even.)"
 * - "At the beginning of your first main phase, put a +1/+1 counter on Sab-Sunen. Then if it has an
 *   odd number of counters on it, draw two cards."
 *
 * The card is a parity clock, so the tests are about parity rather than about counters: fresh (zero,
 * even → can attack), after one trigger (one, odd → drew two, cannot attack), after two (even again).
 *
 * The "Then if …" clause is the subtle one — it resolves *after* the counter is added, not as an
 * intervening-if before it, so the very first trigger draws. An implementation that checked parity
 * first would draw on the opposite turns and pass a naive "does it ever draw" test.
 */
class SabSunenLuxaEmbodiedScenarioTest : ScenarioTestBase() {

    init {
        context("the first-main-phase trigger") {

            test("adds a counter and draws two, because one counter is odd") {
                val game = sabSunenGame()
                val sabSunen = game.findPermanent("Sab-Sunen, Luxa Embodied")!!
                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()

                withClue("the counter went on") {
                    game.state.projectedState.getPower(sabSunen) shouldBe 7
                    game.state.projectedState.getToughness(sabSunen) shouldBe 7
                }
                withClue("one counter is odd, so the post-counter check draws two") {
                    game.handSize(1) shouldBe handBefore + 2
                }
            }
        }

        context("can't attack or block unless it has an even number of counters") {

            test("a fresh Sab-Sunen can attack — zero is even") {
                val game = sabSunenGame()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                withClue("no counters yet, and zero is an even number") {
                    game.declareAttackers(mapOf("Sab-Sunen, Luxa Embodied" to 2)).error shouldBe null
                }
            }

            test("after one trigger it cannot attack — one counter is odd") {
                val game = sabSunenGame()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("one counter is odd, so the attack restriction bites") {
                    game.declareAttackers(mapOf("Sab-Sunen, Luxa Embodied" to 2)).error shouldNotBe null
                }
            }

            test("it cannot block on an odd turn either") {
                // Opponent attacks with a 2/2 while Sab-Sunen sits on one counter.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .also { b -> repeat(12) { b.withCardInLibrary(1, "Grizzly Bears"); b.withCardInLibrary(2, "Grizzly Bears") } }
                    .withCardOnBattlefield(1, "Sab-Sunen, Luxa Embodied", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                // Player 1's own first main phase puts the odd counter on.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()

                // Round the table to the opponent's combat.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.activePlayerId shouldBe game.player2Id
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Sab-Sunen has an odd number of counters, so it can't block") {
                    game.declareBlockers(
                        mapOf("Sab-Sunen, Luxa Embodied" to listOf("Grizzly Bears"))
                    ).error shouldNotBe null
                }
            }

            test("a second trigger restores even parity and it can attack again") {
                val game = sabSunenGame()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()

                // Round the table back to player 1's next first main phase.
                do {
                    game.passUntilPhase(Phase.ENDING, Step.END)
                    game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                } while (game.state.activePlayerId != game.player1Id)
                game.resolveStack()

                val sabSunen = game.findPermanent("Sab-Sunen, Luxa Embodied")!!
                withClue("two counters now") {
                    game.state.projectedState.getPower(sabSunen) shouldBe 8
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("two is even, so it may attack") {
                    game.declareAttackers(mapOf("Sab-Sunen, Luxa Embodied" to 2)).error shouldBe null
                }
            }
        }
    }

    /**
     * Sab-Sunen on player 1's battlefield at the upkeep of player 1's turn — the trigger under test
     * fires on the *first main phase*, so tests advance into it deliberately rather than building
     * there. Libraries are stocked so the two-card draw can't deck anyone.
     */
    private fun sabSunenGame(): TestGame {
        val builder = scenario().withPlayers("Player1", "Player2")
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        builder.withCardOnBattlefield(1, "Sab-Sunen, Luxa Embodied", summoningSickness = false)
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
    }
}
