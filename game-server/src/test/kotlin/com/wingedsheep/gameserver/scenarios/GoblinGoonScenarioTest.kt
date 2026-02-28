package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Scenario tests for Goblin Goon's attack and block restrictions.
 *
 * Goblin Goon: {3}{R} 6/6 Creature — Goblin Mutant
 * "Goblin Goon can't attack unless you control more creatures than defending player."
 * "Goblin Goon can't block unless you control more creatures than attacking player."
 */
class GoblinGoonScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Goon attack restriction") {

            test("Goblin Goon cannot attack when opponent has equal creatures") {
                // Both players have 1 creature each
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Goblin Goon")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val goonId = game.findPermanent("Goblin Goon")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(goonId to game.player2Id))
                )

                withClue("Attack should fail - equal creature count") {
                    attackResult.error shouldNotBe null
                    attackResult.error!! shouldContainIgnoringCase "can't attack"
                }
            }

            test("Goblin Goon cannot attack when opponent has more creatures") {
                // Player 1 has 1 creature, player 2 has 2
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Goblin Goon")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val goonId = game.findPermanent("Goblin Goon")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(goonId to game.player2Id))
                )

                withClue("Attack should fail - opponent has more creatures") {
                    attackResult.error shouldNotBe null
                    attackResult.error!! shouldContainIgnoringCase "can't attack"
                }
            }

            test("Goblin Goon CAN attack when controlling more creatures") {
                // Player 1 has 2 creatures, player 2 has 1
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Goblin Goon")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val goonId = game.findPermanent("Goblin Goon")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(goonId to game.player2Id))
                )

                withClue("Attack should succeed - more creatures than defender: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }

            test("Goblin Goon CAN attack when opponent has no creatures") {
                // Player 1 has 1 creature, player 2 has none
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Goblin Goon")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val goonId = game.findPermanent("Goblin Goon")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(goonId to game.player2Id))
                )

                withClue("Attack should succeed - opponent has no creatures: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }
        }

        context("Goblin Goon block restriction") {

            test("Goblin Goon cannot block when opponent has equal creatures") {
                // Both players have 1 creature each
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Goblin Goon")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val goonId = game.findPermanent("Goblin Goon")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(goonId to listOf(hillGiantId)))
                )

                withClue("Block should fail - equal creature count") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can't block"
                }
            }

            test("Goblin Goon CAN block when controlling more creatures") {
                // Player 2 has 2 creatures, player 1 has 1
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Goblin Goon")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val goonId = game.findPermanent("Goblin Goon")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(goonId to listOf(hillGiantId)))
                )

                withClue("Block should succeed - more creatures than attacker: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }
    }
}
