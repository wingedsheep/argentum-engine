package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goblin War Strike.
 *
 * Card reference:
 * - Goblin War Strike (R): Sorcery
 *   "Goblin War Strike deals damage to target player equal to the number of Goblins you control."
 */
class GoblinWarStrikeScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin War Strike damage calculation") {
            test("deals damage equal to number of Goblins you control") {
                val game = scenario()
                    .withPlayers("Goblin Player", "Opponent")
                    .withCardInHand(1, "Goblin War Strike")
                    .withCardOnBattlefield(1, "Goblin Sky Raider") // Goblin
                    .withCardOnBattlefield(1, "Goblin Sledder")    // Goblin
                    .withCardOnBattlefield(1, "Goblin Warchief")   // Goblin
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellTargetingPlayer(1, "Goblin War Strike", 2)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 3 Goblins you control -> 3 damage to opponent
                withClue("Opponent should have taken 3 damage (20 - 3 = 17)") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("deals zero damage when you control no Goblins") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin War Strike")
                    .withCardOnBattlefield(1, "Hill Giant") // Not a Goblin
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellTargetingPlayer(1, "Goblin War Strike", 2)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 0 Goblins -> 0 damage
                withClue("Opponent should not have taken any damage") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("does not count opponent's Goblins") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin War Strike")
                    .withCardOnBattlefield(1, "Goblin Sky Raider") // Your Goblin
                    .withCardOnBattlefield(2, "Goblin Sledder")    // Opponent's Goblin
                    .withCardOnBattlefield(2, "Goblin Warchief")   // Opponent's Goblin
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellTargetingPlayer(1, "Goblin War Strike", 2)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Only 1 Goblin you control -> 1 damage
                withClue("Opponent should have taken 1 damage (only your Goblins count)") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("can target yourself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin War Strike")
                    .withCardOnBattlefield(1, "Goblin Sky Raider")
                    .withCardOnBattlefield(1, "Goblin Sledder")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellTargetingPlayer(1, "Goblin War Strike", 1)
                withClue("Cast targeting self should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 2 Goblins -> 2 damage to self
                withClue("Player should have taken 2 damage to self") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }
        }
    }
}
