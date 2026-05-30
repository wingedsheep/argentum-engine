package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Dueling Grounds.
 *
 * Card reference:
 * - Dueling Grounds ({1}{G}{W}): Enchantment
 *   "No more than one creature can attack each combat.
 *    No more than one creature can block each combat."
 *
 * Exercises the global [com.wingedsheep.sdk.scripting.AttackerCountLimit] /
 * [com.wingedsheep.sdk.scripting.BlockerCountLimit] static abilities, which constrain the whole
 * declared attacker/blocker set rather than a single creature.
 */
class DuelingGroundsScenarioTest : ScenarioTestBase() {

    init {
        context("Dueling Grounds attacker cap") {

            test("rejects declaring two attackers") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dueling Grounds")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val result = game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2))

                withClue("Declaring two attackers should be rejected by Dueling Grounds") {
                    result.error shouldNotBe null
                }
            }

            test("allows declaring a single attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dueling Grounds")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val result = game.declareAttackers(mapOf("Grizzly Bears" to 2))

                withClue("Declaring one attacker should succeed: ${result.error}") {
                    result.error shouldBe null
                }
            }
        }

        context("Dueling Grounds blocker cap") {

            test("rejects declaring two blockers") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dueling Grounds")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val result = game.declareBlockers(
                    mapOf(
                        "Grizzly Bears" to listOf("Hill Giant"),
                        "Devoted Hero" to listOf("Hill Giant")
                    )
                )

                withClue("Declaring two blockers should be rejected by Dueling Grounds") {
                    result.error shouldNotBe null
                }
            }

            test("allows declaring a single blocker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dueling Grounds")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val result = game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant")))

                withClue("Declaring one blocker should succeed: ${result.error}") {
                    result.error shouldBe null
                }
            }
        }
    }
}
