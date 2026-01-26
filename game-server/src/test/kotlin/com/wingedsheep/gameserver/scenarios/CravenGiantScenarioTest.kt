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
 * Scenario tests for Craven Giant's blocking restriction.
 *
 * Craven Giant: {2}{R} 4/1 Creature - Giant
 * "Craven Giant can't block."
 *
 * These tests verify:
 * 1. Craven Giant cannot block any attacker
 * 2. Other creatures without the restriction CAN block normally
 * 3. Craven Giant CAN attack normally
 */
class CravenGiantScenarioTest : ScenarioTestBase() {

    init {
        context("Craven Giant can't block restriction") {

            test("Craven Giant cannot block an attacker") {
                // Setup:
                // - Player 1 has Devoted Hero (1/1) that will attack
                // - Player 2 has Craven Giant (4/1) as potential blocker
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")   // 1/1 attacker
                    .withCardOnBattlefield(2, "Craven Giant")   // 4/1 "can't block"
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!
                val giantId = game.findPermanent("Craven Giant")!!

                // Declare Devoted Hero as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(heroId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers by passing priority
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to block with Craven Giant - should FAIL (can't block)
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(giantId to listOf(heroId)))
                )

                withClue("Block should fail - Craven Giant can't block") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can't block"
                }
            }

            test("Craven Giant cannot block a large attacker either") {
                // Setup:
                // - Player 1 has Hill Giant (3/3) that will attack
                // - Player 2 has Craven Giant (4/1) as potential blocker
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3 attacker
                    .withCardOnBattlefield(2, "Craven Giant")   // 4/1 "can't block"
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val cravenGiantId = game.findPermanent("Craven Giant")!!

                // Declare Hill Giant as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers by passing priority
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to block with Craven Giant - should FAIL (can't block)
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(cravenGiantId to listOf(hillGiantId)))
                )

                withClue("Block should fail - Craven Giant can't block") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can't block"
                }
            }

            test("Craven Giant CAN attack normally") {
                // Setup:
                // - Player 1 has Craven Giant (4/1) that will attack
                // - Player 2 has Grizzly Bears (2/2) as potential blocker
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Craven Giant")   // 4/1 attacker (can attack)
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val giantId = game.findPermanent("Craven Giant")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare Craven Giant as attacker - should SUCCEED
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(giantId to game.player2Id))
                )
                withClue("Craven Giant should be able to attack: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers by passing priority
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Grizzly Bears - should SUCCEED (normal creature blocking)
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(giantId)))
                )

                withClue("Block should succeed - Grizzly Bears can block: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("normal creature CAN block when Craven Giant is also on battlefield") {
                // Setup:
                // - Player 1 has Devoted Hero (1/1) that will attack
                // - Player 2 has both Craven Giant (4/1) and Grizzly Bears (2/2)
                // - Only Grizzly Bears should be able to block
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")   // 1/1 attacker
                    .withCardOnBattlefield(2, "Craven Giant")   // 4/1 "can't block"
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2 normal blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare Devoted Hero as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(heroId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers by passing priority
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Grizzly Bears - should SUCCEED
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(heroId)))
                )

                withClue("Block should succeed - Grizzly Bears can block: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }
    }
}
