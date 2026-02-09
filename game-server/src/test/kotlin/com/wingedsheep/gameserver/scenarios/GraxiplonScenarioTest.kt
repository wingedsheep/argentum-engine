package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Graxiplon.
 *
 * Card reference:
 * - Graxiplon ({5}{U}): Creature — Beast, 3/4
 *   "Graxiplon can't be blocked unless defending player controls three or more
 *    creatures that share a creature type."
 *
 * Tests:
 * 1. Cannot be blocked when defender has creatures with no shared type
 * 2. Cannot be blocked when defender has only 2 creatures sharing a type
 * 3. CAN be blocked when defender has 3+ creatures sharing a type
 * 4. Any of the defender's creatures can block (not just the shared-type ones)
 */
class GraxiplonScenarioTest : ScenarioTestBase() {

    init {
        context("Graxiplon blocking restriction") {

            test("cannot be blocked when defender has no creatures sharing a type") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Graxiplon")
                    .withCardOnBattlefield(2, "Grizzly Bears")   // Bear
                    .withCardOnBattlefield(2, "Elvish Warrior")  // Elf Warrior
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Graxiplon" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Graxiplon")
                ))
                withClue("Should not be able to block Graxiplon without 3 creatures sharing a type") {
                    blockResult.error shouldNotBe null
                }
            }

            test("cannot be blocked when defender has only 2 creatures sharing a type") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Graxiplon")
                    .withCardOnBattlefield(2, "Skirk Prospector")  // Goblin
                    .withCardOnBattlefield(2, "Sparksmith")        // Goblin
                    .withCardOnBattlefield(2, "Grizzly Bears")     // Bear (not Goblin)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Graxiplon" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Graxiplon")
                ))
                withClue("Should not be able to block Graxiplon with only 2 creatures sharing a type") {
                    blockResult.error shouldNotBe null
                }
            }

            test("CAN be blocked when defender has 3 creatures sharing a type") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Graxiplon")
                    .withCardOnBattlefield(2, "Skirk Prospector")  // Goblin
                    .withCardOnBattlefield(2, "Sparksmith")        // Goblin
                    .withCardOnBattlefield(2, "Goblin Taskmaster") // Goblin (3rd)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Graxiplon" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Skirk Prospector" to listOf("Graxiplon")
                ))
                withClue("Should be able to block Graxiplon when defender has 3 Goblins: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("any creature can block when condition is met, not just the shared-type ones") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Graxiplon")
                    .withCardOnBattlefield(2, "Skirk Prospector")  // Goblin
                    .withCardOnBattlefield(2, "Sparksmith")        // Goblin
                    .withCardOnBattlefield(2, "Goblin Taskmaster") // Goblin (3rd)
                    .withCardOnBattlefield(2, "Grizzly Bears")     // Bear (not Goblin)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Graxiplon" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // The Bear can block too, since the condition is met by the 3 Goblins
                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Graxiplon")
                ))
                withClue("Non-Goblin creature should be able to block when condition is met: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }
    }
}
