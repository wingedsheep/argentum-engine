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
 * Scenario tests for Spitfire Handler.
 *
 * Spitfire Handler: {1}{R} 1/1 Creature — Goblin
 * "This creature can't block creatures with power greater than this creature's power."
 * "{R}: This creature gets +1/+0 until end of turn."
 *
 * These tests verify:
 * 1. Spitfire Handler can't block creatures with power > its power
 * 2. Spitfire Handler CAN block creatures with power <= its power
 * 3. After pumping, it can block larger creatures
 * 4. Spitfire Handler CAN attack normally
 */
class SpitfireHandlerScenarioTest : ScenarioTestBase() {

    init {
        context("Spitfire Handler blocking restriction") {

            test("cannot block a creature with power greater than its own") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")      // 2/2 attacker
                    .withCardOnBattlefield(2, "Spitfire Handler")   // 1/1 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val handlerId = game.findPermanent("Spitfire Handler")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(bearsId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to block with Spitfire Handler (1/1) against Grizzly Bears (2/2) - should FAIL
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(handlerId to listOf(bearsId)))
                )

                withClue("Block should fail - Spitfire Handler can't block creature with greater power") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can't block"
                    blockResult.error!! shouldContainIgnoringCase "power"
                }
            }

            test("CAN block a creature with power equal to its own") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")       // 1/1 attacker
                    .withCardOnBattlefield(2, "Spitfire Handler")   // 1/1 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!
                val handlerId = game.findPermanent("Spitfire Handler")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(heroId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Spitfire Handler (1/1) against Devoted Hero (1/1) - should SUCCEED
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(handlerId to listOf(heroId)))
                )

                withClue("Block should succeed - attacker power equals blocker power: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("CAN attack normally") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Spitfire Handler")   // 1/1 attacker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handlerId = game.findPermanent("Spitfire Handler")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(handlerId to game.player2Id))
                )
                withClue("Spitfire Handler should be able to attack: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }
        }

        context("Spitfire Handler activated ability") {

            test("pumping allows blocking larger creatures") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")      // 2/2 attacker
                    .withCardOnBattlefield(2, "Spitfire Handler")   // 1/1 blocker
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val handlerId = game.findPermanent("Spitfire Handler")!!

                // Declare attackers
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(bearsId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Player 1 passes priority so player 2 can activate abilities
                game.execute(PassPriority(game.player1Id))

                // Player 2 activates Spitfire Handler's {R}: +1/+0 ability twice
                val cardDef = cardRegistry.getCard("Spitfire Handler")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activate1 = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = handlerId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )
                withClue("First activation should succeed: ${activate1.error}") {
                    activate1.error shouldBe null
                }

                // Resolve first activation
                game.resolveStack()

                // After resolution, active player (P1) has priority - pass to P2
                game.execute(PassPriority(game.player1Id))

                val activate2 = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = handlerId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )
                withClue("Second activation should succeed: ${activate2.error}") {
                    activate2.error shouldBe null
                }

                // Resolve second activation
                game.resolveStack()

                // Now advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Spitfire Handler (now 3/1) against Grizzly Bears (2/2) - should SUCCEED
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(handlerId to listOf(bearsId)))
                )

                withClue("Block should succeed - pumped handler power (3) >= attacker power (2): ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }
    }
}
