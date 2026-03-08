package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ride Down.
 *
 * Card reference:
 * - Ride Down ({R}{W}): Instant
 *   "Destroy target blocking creature. Creatures that were blocked by that creature
 *    this combat gain trample until end of turn."
 */
class RideDownScenarioTest : ScenarioTestBase() {

    init {
        context("Ride Down") {
            test("destroys blocking creature and grants trample to blocked attacker") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Tusked Colossodon") // 6/5 attacker
                    .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2 blocker
                    .withCardInHand(1, "Ride Down")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val attackerId = game.findPermanent("Tusked Colossodon")!!
                val blockerId = game.findPermanent("Grizzly Bears")!!

                // Declare attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(attackerId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declare blocker
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(blockerId to listOf(attackerId)))
                )
                withClue("Blocking should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // P2 has priority after declaring blockers, pass to P1
                game.passPriority()

                // Cast Ride Down targeting the blocking creature
                val castResult = game.castSpell(1, "Ride Down", blockerId)
                withClue("Ride Down should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Blocker should be destroyed
                withClue("Grizzly Bears should be destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                // Attacker should have trample
                val projected = game.state.projectedState
                withClue("Tusked Colossodon should have trample") {
                    projected.hasKeyword(attackerId, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("grants trample to multiple attackers blocked by same creature") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")   // 1/2 attacker 1
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 attacker 2
                    .withCardOnBattlefield(2, "Brave the Sands") // allows blocking additional creature
                    .withCardOnBattlefield(2, "Tusked Colossodon") // 6/5 blocker (blocks both)
                    .withCardInHand(1, "Ride Down")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hero = game.findPermanent("Devoted Hero")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val blockerId = game.findPermanent("Tusked Colossodon")!!

                // Declare both attackers
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        hero to game.player2Id,
                        bears to game.player2Id
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block both attackers with Tusked Colossodon (Brave the Sands allows this)
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(blockerId to listOf(hero, bears)))
                )

                // P2 has priority after declaring blockers, pass to P1
                game.passPriority()

                // Cast Ride Down targeting the blocker
                val castResult = game.castSpell(1, "Ride Down", blockerId)
                withClue("Ride Down should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Blocker should be destroyed
                withClue("Tusked Colossodon should be destroyed") {
                    game.isOnBattlefield("Tusked Colossodon") shouldBe false
                }

                // Both attackers should have trample
                val projected = game.state.projectedState
                withClue("Devoted Hero should have trample") {
                    projected.hasKeyword(hero, Keyword.TRAMPLE) shouldBe true
                }
                withClue("Grizzly Bears should have trample") {
                    projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
            }
        }
    }
}
