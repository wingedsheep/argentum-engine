package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Final Punishment.
 *
 * Card reference:
 * - Final Punishment (3BB): Sorcery
 *   "Target player loses life equal to the damage already dealt to that player this turn."
 */
class FinalPunishmentScenarioTest : ScenarioTestBase() {

    init {
        context("Final Punishment") {
            test("loses life equal to damage dealt this turn") {
                // Use Scorching Spear ({R}, 1 damage to any target) to deal damage first
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Scorching Spear")
                    .withCardInHand(1, "Final Punishment")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Deal 1 damage with Scorching Spear targeting opponent
                val spearResult = game.castSpellTargetingPlayer(1, "Scorching Spear", 2)
                withClue("Scorching Spear cast should succeed") {
                    spearResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Opponent should be at 19 after Scorching Spear") {
                    game.getLifeTotal(2) shouldBe 19
                }

                // Now cast Final Punishment targeting opponent
                val fpResult = game.castSpellTargetingPlayer(1, "Final Punishment", 2)
                withClue("Final Punishment cast should succeed") {
                    fpResult.error shouldBe null
                }
                game.resolveStack()

                // Opponent loses 1 more life (equal to 1 damage dealt this turn)
                withClue("Opponent should lose 1 more life (19 - 1 = 18)") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("loses zero life when no damage dealt this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Final Punishment")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpellTargetingPlayer(1, "Final Punishment", 2)
                withClue("Cast should succeed") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Opponent should still be at 20 (no damage dealt this turn)") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("can target yourself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Scorching Spear")
                    .withCardInHand(1, "Final Punishment")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Scorching Spear yourself for 1
                val spearResult = game.castSpellTargetingPlayer(1, "Scorching Spear", 1)
                withClue("Scorching Spear cast should succeed") {
                    spearResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Player should be at 19 after Scorching Spear") {
                    game.getLifeTotal(1) shouldBe 19
                }

                // Final Punishment targeting self
                val fpResult = game.castSpellTargetingPlayer(1, "Final Punishment", 1)
                withClue("Final Punishment cast should succeed") {
                    fpResult.error shouldBe null
                }
                game.resolveStack()

                // Player loses 1 more life
                withClue("Player should lose 1 more (19 - 1 = 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }
        }
    }
}
