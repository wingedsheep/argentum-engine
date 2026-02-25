package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for double strike combat damage.
 *
 * Double strike creatures deal combat damage twice: once during the first strike
 * combat damage step and again during the regular combat damage step.
 */
class RidgetopRaptorScenarioTest : ScenarioTestBase() {

    init {
        context("Double strike combat damage") {

            test("double strike creature deals damage twice to defending player when unblocked") {
                // Ridgetop Raptor (2/1 double strike) attacks unblocked
                // Should deal 2 first strike + 2 regular = 4 total damage
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Ridgetop Raptor")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val raptorId = game.findPermanent("Ridgetop Raptor")!!
                val startingLife = game.getLifeTotal(2)

                // Declare Ridgetop Raptor as attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(raptorId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // No blockers
                game.execute(DeclareBlockers(game.player2Id, emptyMap()))

                // Advance through combat damage to postcombat main
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Should have dealt 4 total damage (2 first strike + 2 regular)
                withClue("Defending player should take 4 total damage from double strike (2+2)") {
                    game.getLifeTotal(2) shouldBe startingLife - 4
                }
            }

            test("double strike creature kills blocker in first strike step, survives") {
                // Ridgetop Raptor (2/1 double strike) blocked by Glory Seeker (2/2)
                // First strike damage: 2 to Glory Seeker (lethal)
                // Glory Seeker dies before dealing regular combat damage back
                // Ridgetop Raptor survives
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Ridgetop Raptor")  // 2/1 double strike
                    .withCardOnBattlefield(2, "Glory Seeker")      // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val raptorId = game.findPermanent("Ridgetop Raptor")!!
                val seekerId = game.findPermanent("Glory Seeker")!!

                // Declare Ridgetop Raptor as attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(raptorId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Glory Seeker
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(seekerId to listOf(raptorId)))
                )

                // Advance through combat
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Glory Seeker should be dead (2 first strike damage >= 2 toughness)
                withClue("Glory Seeker should be dead from first strike damage") {
                    game.findPermanent("Glory Seeker") shouldBe null
                    game.isInGraveyard(2, "Glory Seeker") shouldBe true
                }

                // Ridgetop Raptor should survive (Glory Seeker dies before dealing damage)
                withClue("Ridgetop Raptor should survive (blocker died in first strike step)") {
                    game.findPermanent("Ridgetop Raptor") shouldNotBe null
                }
            }

            test("double strike creature dies to larger blocker in regular damage step") {
                // Ridgetop Raptor (2/1 double strike) blocked by Hill Giant (3/3)
                // First strike damage: 2 to Hill Giant (not lethal, 3 toughness)
                // Regular damage: both deal damage simultaneously
                //   Raptor deals 2 more to Hill Giant (total 4, lethal)
                //   Hill Giant deals 3 to Raptor (lethal for 1 toughness)
                // Both should die
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Ridgetop Raptor")  // 2/1 double strike
                    .withCardOnBattlefield(2, "Hill Giant")        // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val raptorId = game.findPermanent("Ridgetop Raptor")!!
                val giantId = game.findPermanent("Hill Giant")!!

                // Declare Ridgetop Raptor as attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(raptorId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Hill Giant
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(giantId to listOf(raptorId)))
                )

                // Advance through combat
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Hill Giant should be dead (2 first strike + 2 regular = 4 total >= 3 toughness)
                withClue("Hill Giant should be dead from accumulated double strike damage") {
                    game.findPermanent("Hill Giant") shouldBe null
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }

                // Ridgetop Raptor should also be dead (3 damage from Hill Giant >= 1 toughness)
                withClue("Ridgetop Raptor should be dead from Hill Giant's regular combat damage") {
                    game.findPermanent("Ridgetop Raptor") shouldBe null
                    game.isInGraveyard(1, "Ridgetop Raptor") shouldBe true
                }
            }
        }
    }
}
