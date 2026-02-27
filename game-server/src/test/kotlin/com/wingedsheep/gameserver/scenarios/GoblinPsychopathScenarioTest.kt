package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tests for Goblin Psychopath's coin flip combat damage redirection.
 *
 * Goblin Psychopath: {3}{R}
 * Creature — Goblin Mutant 5/5
 * Whenever Goblin Psychopath attacks or blocks, flip a coin. If you lose
 * the flip, the next time it would deal combat damage this turn, it deals
 * that damage to you instead.
 */
class GoblinPsychopathScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Psychopath attacking unblocked") {

            test("losing the flip redirects combat damage to controller") {
                var foundLostFlip = false
                repeat(50) {
                    if (foundLostFlip) return@repeat

                    val game = scenario()
                        .withPlayers("Attacker", "Defender")
                        .withCardOnBattlefield(1, "Goblin Psychopath", summoningSickness = false)
                        .withCardInLibrary(1, "Mountain")
                        .withCardInLibrary(2, "Mountain")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()

                    // Move to declare attackers
                    game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    game.declareAttackers(mapOf("Goblin Psychopath" to 2))

                    // Resolve the attack trigger (coin flip)
                    val results = game.resolveStack()
                    val coinEvent = results.flatMap { it.events }
                        .filterIsInstance<CoinFlipEvent>().firstOrNull()
                        ?: return@repeat

                    if (!coinEvent.won) {
                        // Lost the flip - pass through combat damage
                        game.declareNoBlockers()
                        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                        withClue("Attacker should have taken 5 damage (redirected)") {
                            game.getLifeTotal(1) shouldBe 15
                        }
                        withClue("Defender should be unharmed") {
                            game.getLifeTotal(2) shouldBe 20
                        }
                        foundLostFlip = true
                    }
                }
                withClue("Should have found a lost flip within 50 attempts") {
                    foundLostFlip shouldBe true
                }
            }

            test("winning the flip deals normal combat damage") {
                var foundWonFlip = false
                repeat(50) {
                    if (foundWonFlip) return@repeat

                    val game = scenario()
                        .withPlayers("Attacker", "Defender")
                        .withCardOnBattlefield(1, "Goblin Psychopath", summoningSickness = false)
                        .withCardInLibrary(1, "Mountain")
                        .withCardInLibrary(2, "Mountain")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()

                    game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    game.declareAttackers(mapOf("Goblin Psychopath" to 2))

                    val results = game.resolveStack()
                    val coinEvent = results.flatMap { it.events }
                        .filterIsInstance<CoinFlipEvent>().firstOrNull()
                        ?: return@repeat

                    if (coinEvent.won) {
                        game.declareNoBlockers()
                        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                        withClue("Defender should have taken 5 combat damage") {
                            game.getLifeTotal(2) shouldBe 15
                        }
                        withClue("Attacker should be unharmed") {
                            game.getLifeTotal(1) shouldBe 20
                        }
                        foundWonFlip = true
                    }
                }
                withClue("Should have found a won flip within 50 attempts") {
                    foundWonFlip shouldBe true
                }
            }
        }

        context("Goblin Psychopath blocking") {

            test("losing the flip redirects blocker's combat damage to its controller") {
                var foundLostFlip = false
                repeat(50) {
                    if (foundLostFlip) return@repeat

                    val game = scenario()
                        .withPlayers("Attacker", "Blocker")
                        .withCardOnBattlefield(1, "Goblin Brigand", summoningSickness = false)
                        .withCardOnBattlefield(2, "Goblin Psychopath", summoningSickness = false)
                        .withCardInLibrary(1, "Mountain")
                        .withCardInLibrary(2, "Mountain")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()

                    // Attack with Goblin Brigand (2/2)
                    game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    game.declareAttackers(mapOf("Goblin Brigand" to 2))

                    // Move to declare blockers
                    game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                    // Block with Goblin Psychopath (5/5)
                    game.declareBlockers(mapOf("Goblin Psychopath" to listOf("Goblin Brigand")))

                    // Resolve the block trigger (coin flip)
                    val results = game.resolveStack()
                    val coinEvent = results.flatMap { it.events }
                        .filterIsInstance<CoinFlipEvent>().firstOrNull()
                        ?: return@repeat

                    if (!coinEvent.won) {
                        // Lost the flip - pass through combat damage
                        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                        // Goblin Psychopath's controller (P2) should take 5 damage (redirected)
                        withClue("Blocker's controller should have taken 5 damage (redirected)") {
                            game.getLifeTotal(2) shouldBe 15
                        }
                        foundLostFlip = true
                    }
                }
                withClue("Should have found a lost flip within 50 attempts") {
                    foundLostFlip shouldBe true
                }
            }
        }
    }
}
