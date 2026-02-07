package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Elven Riders.
 *
 * Card reference:
 * - Elven Riders ({3}{G}{G}): Creature — Elf, 3/3
 *   "Elven Riders can't be blocked except by creatures with flying."
 *
 * Tests:
 * 1. Non-flying creature cannot block Elven Riders
 * 2. Flying creature CAN block Elven Riders
 * 3. Reach creature CAN block Elven Riders (per MTG rules)
 */
class ElvenRidersScenarioTest : ScenarioTestBase() {

    init {
        context("Elven Riders blocking restrictions") {

            test("non-flying creature cannot block Elven Riders") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Elven Riders")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2, no flying
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Elven Riders" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Elven Riders")
                ))
                withClue("Non-flying creature should not be able to block Elven Riders") {
                    blockResult.error shouldNotBe null
                }
            }

            test("flying creature CAN block Elven Riders") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Elven Riders")
                    .withCardOnBattlefield(2, "Goblin Sky Raider") // 1/2 flying, no blocking restriction
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Elven Riders" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Goblin Sky Raider" to listOf("Elven Riders")
                ))
                withClue("Flying creature should be able to block Elven Riders: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("reach creature CAN block Elven Riders") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Elven Riders")
                    .withCardOnBattlefield(2, "Spitting Gourna") // 2/4 reach (Onslaught beast)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Elven Riders" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Spitting Gourna" to listOf("Elven Riders")
                ))
                withClue("Reach creature should be able to block Elven Riders: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }
    }
}
