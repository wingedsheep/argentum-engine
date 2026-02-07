package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Righteous Cause.
 *
 * Card reference:
 * - Righteous Cause ({3}{W}{W}): Enchantment
 *   "Whenever a creature attacks, you gain 1 life."
 */
class RighteousCauseScenarioTest : ScenarioTestBase() {

    init {
        context("Righteous Cause - life gain on attack") {
            test("controller gains 1 life when a single creature attacks") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Righteous Cause")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))

                // Resolve triggered ability
                game.resolveStack()

                withClue("Player1 should have gained 1 life from Righteous Cause") {
                    game.getLifeTotal(1) shouldBe startingLife + 1
                }
            }

            test("controller gains life for each attacking creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Righteous Cause")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(1, "Elvish Warrior") // 2/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2, "Elvish Warrior" to 2))

                // Advance to blockers step - resolves all triggers along the way
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Player1 should have gained 2 life (1 per attacker)") {
                    game.getLifeTotal(1) shouldBe startingLife + 2
                }
            }

            test("triggers when opponent's creatures attack too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Righteous Cause")
                    .withCardOnBattlefield(2, "Glory Seeker") // opponent's creature
                    .withActivePlayer(2) // opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))

                // Resolve triggered ability
                game.resolveStack()

                withClue("Player1 should have gained 1 life even when opponent attacks") {
                    game.getLifeTotal(1) shouldBe startingLife + 1
                }
            }
        }
    }
}
