package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for multi-blocker combat-damage assignment at the server level.
 *
 * Damage-assignment order (CR 510.1c) is no longer a standalone OrderObjectsDecision pre-step —
 * it's folded into the combat resolution board: declaration order is the default, and the board
 * carries any reorder. These tests verify the server surfaces the board for a real division
 * choice, resolves single-blocker combat without one, and applies damage lethal-first in
 * declaration order. The full ordering matrix is covered by the engine's CombatResolutionBoardTest.
 */
class BlockerOrderScenarioTest : ScenarioTestBase() {

    init {
        context("Multi-blocker combat") {
            test("a multi-blocked trample attacker surfaces a combat resolution board") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Blistering Firecat") // 7/1 trample haste
                    .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2 (lethal 2)
                    .withCardOnBattlefield(2, "Devoted Hero")       // 1/2 (lethal 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val tramplerId = game.findPermanent("Blistering Firecat")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                game.execute(DeclareAttackers(game.player1Id, mapOf(tramplerId to game.player2Id)))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(
                        bearsId to listOf(tramplerId),
                        heroId to listOf(tramplerId)
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                val decision = game.getPendingDecision()
                withClue("Multi-blocked trample attacker should surface a combat resolution board") {
                    decision shouldNotBe null
                    decision.shouldBeInstanceOf<CombatResolutionDecision>()
                }
                decision as CombatResolutionDecision
                withClue("Board should be for the attacking player and expose both blocker edges + a drain") {
                    decision.playerId shouldBe game.player1Id
                    decision.edges.filter {
                        it.sourceId == tramplerId && it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER
                    }.map { it.targetId }.toSet() shouldBe setOf(bearsId, heroId)
                    decision.edges.any { it.sourceId == tramplerId && it.isTrampleDrain } shouldBe true
                }

                // Confirm defaults: 2 to bears (lethal), 2 to hero (lethal), 3 trample to player.
                game.submitDefaultCombatDamage()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Both blockers die to the lethal-first default") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.findPermanent("Devoted Hero") shouldBe null
                }
                withClue("Defender takes 3 trample damage (7 power - 2 - 2 lethal)") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("single blocker resolves with no decision") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")       // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.execute(DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id)))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(hillGiantId)))
                )
                withClue("Single blocker, no trample: no board decision needed") {
                    blockResult.error shouldBe null
                    blockResult.pendingDecision shouldBe null
                }
            }

            test("non-trample multi-block auto-resolves lethal-first in declaration order") {
                // Glory Seeker (2/2) blocked by Devoted Hero (2/1) then Grizzly Bears (2/2).
                // 2 power, total lethal 3 — no excess and no trample, so combat auto-resolves with
                // no board. Lethal-first in declaration order kills the first blocker; the second
                // gets the leftover (1, not lethal) and survives.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 attacker
                    .withCardOnBattlefield(2, "Devoted Hero")     // 2/1 (declared first)
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2 (declared second)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val seekerId = game.findPermanent("Glory Seeker")!!
                val heroId = game.findPermanent("Devoted Hero")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.execute(DeclareAttackers(game.player1Id, mapOf(seekerId to game.player2Id)))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(
                        heroId to listOf(seekerId),
                        bearsId to listOf(seekerId)
                    ))
                )

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("First-ordered blocker (Devoted Hero) took lethal and died") {
                    game.findPermanent("Devoted Hero") shouldBe null
                }
                withClue("Second-ordered blocker (Grizzly Bears) only got the leftover and survived") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
                withClue("Glory Seeker died to 4 damage from the two blockers") {
                    game.findPermanent("Glory Seeker") shouldBe null
                }
            }
        }
    }
}
