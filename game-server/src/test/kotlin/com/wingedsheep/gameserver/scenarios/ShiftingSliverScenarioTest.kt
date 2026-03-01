package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Shifting Sliver.
 *
 * Card reference:
 * - Shifting Sliver ({3}{U}): Creature — Sliver, 2/2
 *   "Slivers can't be blocked except by Slivers."
 *
 * Tests:
 * 1. A Sliver cannot be blocked by a non-Sliver creature when Shifting Sliver is on the battlefield
 * 2. A Sliver CAN be blocked by another Sliver creature
 * 3. The evasion applies to all Slivers, not just Shifting Sliver itself
 */
class ShiftingSliverScenarioTest : ScenarioTestBase() {

    init {
        context("Shifting Sliver blocking restrictions") {

            test("non-Sliver creature cannot block a Sliver when Shifting Sliver is on the battlefield") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Shifting Sliver")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2, not a Sliver
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shifting Sliver" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Shifting Sliver")
                ))
                withClue("Non-Sliver creature should not be able to block a Sliver") {
                    blockResult.error shouldNotBe null
                }
            }

            test("Sliver creature CAN block a Sliver when Shifting Sliver is on the battlefield") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Shifting Sliver")
                    .withCardOnBattlefield(2, "Blade Sliver") // 2/2 Sliver
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shifting Sliver" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Blade Sliver" to listOf("Shifting Sliver")
                ))
                withClue("Sliver creature should be able to block a Sliver: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("evasion applies to other Slivers, not just Shifting Sliver") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Shifting Sliver")
                    .withCardOnBattlefield(1, "Blade Sliver") // Another Sliver attacking
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2, not a Sliver
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Blade Sliver" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Blade Sliver")
                ))
                withClue("Non-Sliver creature should not be able to block any Sliver") {
                    blockResult.error shouldNotBe null
                }
            }
        }
    }
}
