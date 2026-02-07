package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Leery Fogbeast.
 *
 * Leery Fogbeast: {2}{G} 4/2 Creature — Beast
 * "Whenever Leery Fogbeast becomes blocked, prevent all combat damage that would be dealt this turn."
 *
 * These tests verify:
 * 1. When Leery Fogbeast attacks and is blocked, all combat damage is prevented
 * 2. When Leery Fogbeast attacks unblocked, combat damage is dealt normally
 * 3. Other creatures' combat damage is also prevented when Fogbeast becomes blocked
 */
class LeeryFogbeastScenarioTest : ScenarioTestBase() {

    init {
        context("Leery Fogbeast becomes blocked") {

            test("when blocked, all combat damage is prevented - no creatures die") {
                // Setup:
                // - Player 1 attacks with Leery Fogbeast (4/2)
                // - Player 2 blocks with Grizzly Bears (2/2)
                // - All combat damage should be prevented: both survive
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Leery Fogbeast")  // 4/2
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val fogbeastId = game.findPermanent("Leery Fogbeast")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare Fogbeast as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(fogbeastId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block Fogbeast with Grizzly Bears - triggers "becomes blocked"
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(fogbeastId)))
                )
                withClue("Block should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Advance through combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Both creatures should survive since all combat damage was prevented
                withClue("Leery Fogbeast should survive (combat damage prevented)") {
                    game.findPermanent("Leery Fogbeast") shouldBe fogbeastId
                }
                withClue("Grizzly Bears should survive (combat damage prevented)") {
                    game.findPermanent("Grizzly Bears") shouldBe bearsId
                }
            }

            test("when unblocked, deals combat damage to defending player normally") {
                // Setup:
                // - Player 1 attacks with Leery Fogbeast (4/2)
                // - Player 2 doesn't block
                // - Fogbeast should deal 4 damage to player 2
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Leery Fogbeast")  // 4/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val fogbeastId = game.findPermanent("Leery Fogbeast")!!
                val startingLife = game.getLifeTotal(2)

                // Declare Fogbeast as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(fogbeastId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance through combat (no blockers declared, so goes straight through)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Player 2 should have taken 4 damage
                val endingLife = game.getLifeTotal(2)
                withClue("Defending player should take 4 damage from unblocked Fogbeast") {
                    endingLife shouldBe startingLife - 4
                }
            }

            test("other creatures' combat damage is also prevented when Fogbeast becomes blocked") {
                // Setup:
                // - Player 1 attacks with Leery Fogbeast (4/2) AND Hill Giant (3/3)
                // - Player 2 blocks Fogbeast with Grizzly Bears (2/2), Hill Giant unblocked
                // - All combat damage prevented: Hill Giant doesn't deal damage either
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Leery Fogbeast")  // 4/2
                    .withCardOnBattlefield(1, "Hill Giant")      // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val fogbeastId = game.findPermanent("Leery Fogbeast")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val startingLife = game.getLifeTotal(2)

                // Declare both as attackers
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        fogbeastId to game.player2Id,
                        hillGiantId to game.player2Id
                    ))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block only Fogbeast with Grizzly Bears
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(fogbeastId)))
                )
                withClue("Block should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Advance through combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // All creatures should survive
                withClue("Leery Fogbeast should survive") {
                    game.findPermanent("Leery Fogbeast") shouldBe fogbeastId
                }
                withClue("Grizzly Bears should survive") {
                    game.findPermanent("Grizzly Bears") shouldBe bearsId
                }

                // Player 2's life should be unchanged (Hill Giant's damage also prevented)
                val endingLife = game.getLifeTotal(2)
                withClue("Defending player should take no damage (all combat damage prevented)") {
                    endingLife shouldBe startingLife
                }
            }
        }
    }
}
