package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Exalted Angel.
 *
 * Card reference:
 * - Exalted Angel ({4}{W}{W}): Creature — Angel, 4/5
 *   Flying
 *   Whenever Exalted Angel deals damage, you gain that much life.
 *   Morph {2}{W}{W}
 */
class ExaltedAngelScenarioTest : ScenarioTestBase() {

    init {
        context("Exalted Angel combat damage trigger") {
            test("gains life equal to combat damage dealt to player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Exalted Angel")
                    .withActivePlayer(1)
                    .withLifeTotal(1, 15)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat and attack
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Exalted Angel" to 2))

                // Pass through declare blockers (opponent declares no blockers)
                game.passPriority()
                game.declareNoBlockers()

                // Pass through to combat damage
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Trigger should fire and resolve - gain 4 life from dealing 4 damage
                game.resolveStack()

                withClue("Player should gain life equal to damage dealt (4)") {
                    game.getLifeTotal(1) shouldBe 19 // 15 + 4
                }
                withClue("Opponent should lose life from combat damage") {
                    game.getLifeTotal(2) shouldBe 16 // 20 - 4
                }
            }

            test("gains life when dealing combat damage to a blocking creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Exalted Angel")
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 can block
                    .withActivePlayer(1)
                    .withLifeTotal(1, 15)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat and attack
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Exalted Angel" to 2))

                // Opponent blocks with Towering Baloth
                game.passPriority()
                game.declareBlockers(mapOf("Towering Baloth" to listOf("Exalted Angel")))

                // Pass through to combat damage
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Trigger should fire - Exalted Angel dealt 4 damage to the blocker
                game.resolveStack()

                withClue("Player should gain 4 life from damage dealt to blocker") {
                    game.getLifeTotal(1) shouldBe 19 // 15 + 4
                }
            }
        }
    }
}
