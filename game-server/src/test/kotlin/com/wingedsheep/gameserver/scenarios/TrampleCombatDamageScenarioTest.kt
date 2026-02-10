package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf

/**
 * Scenario tests for trample combat damage assignment.
 *
 * Tests the new AssignDamageDecision flow where the attacking player
 * chooses how to distribute damage from a trampling creature among
 * the blocker(s) and the defending player.
 */
class TrampleCombatDamageScenarioTest : ScenarioTestBase() {

    init {
        context("Trample combat damage assignment") {

            test("trample creature blocked by smaller creature - default assignment kills blocker, excess to player") {
                // Blistering Firecat (7/1 trample) blocked by Glory Seeker (2/2)
                // Default: 2 to blocker (lethal), 5 to player
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Blistering Firecat") // 7/1 trample haste
                    .withCardOnBattlefield(2, "Glory Seeker")       // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val firecatId = game.findPermanent("Blistering Firecat")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val startingLife = game.getLifeTotal(2)

                // Declare Blistering Firecat as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(firecatId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Glory Seeker
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(seekerId to listOf(firecatId)))
                )
                withClue("Block should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Advance to combat damage step - should present AssignDamageDecision
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                withClue("Should have damage assignment decision for trample creature") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision() shouldBe beInstanceOf<AssignDamageDecision>()
                }

                val decision = game.getPendingDecision() as AssignDamageDecision
                withClue("Decision should have correct fields") {
                    decision.attackerId shouldBe firecatId
                    decision.availablePower shouldBe 7
                    decision.hasTrample shouldBe true
                }

                // Accept the default assignment (2 to blocker, 5 to player)
                game.submitDefaultDamageAssignment()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Glory Seeker should be dead
                withClue("Glory Seeker should be dead") {
                    game.findPermanent("Glory Seeker") shouldBe null
                    game.isInGraveyard(2, "Glory Seeker") shouldBe true
                }

                // Defending player takes 5 trample damage
                withClue("Defending player should take 5 trample damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 5
                }
            }

            test("trample creature - custom assignment puts more damage on blocker") {
                // Blistering Firecat (7/1 trample) blocked by Glory Seeker (2/2)
                // Custom: 6 to blocker, 1 to player
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Blistering Firecat") // 7/1 trample haste
                    .withCardOnBattlefield(2, "Glory Seeker")       // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val firecatId = game.findPermanent("Blistering Firecat")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val startingLife = game.getLifeTotal(2)

                // Declare attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(firecatId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Glory Seeker
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(seekerId to listOf(firecatId)))
                )

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Custom assignment: 6 to blocker, 1 to player
                val decision = game.getPendingDecision() as AssignDamageDecision
                game.submitDamageAssignment(
                    mapOf(seekerId to 6, game.player2Id to 1)
                )

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Glory Seeker still dies (6 >> 2 toughness)
                withClue("Glory Seeker should be dead") {
                    game.findPermanent("Glory Seeker") shouldBe null
                }

                // Only 1 damage to player
                withClue("Defending player should take 1 trample damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 1
                }
            }

            test("Daunting Defender - default assignment accounts for prevention and kills Cleric") {
                // Blistering Firecat (7/1 trample) blocked by Daunting Defender (3/3 Cleric)
                // Daunting Defender prevents 1 of every damage to Clerics (including itself).
                // Default assignment accounts for prevention: 4 to blocker (3 toughness + 1 prevention), 3 to player
                // With prevention: 4 assigned - 1 prevented = 3 dealt = lethal for 3/3
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Blistering Firecat")  // 7/1 trample haste
                    .withCardOnBattlefield(2, "Daunting Defender")   // 3/3 Human Cleric
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val firecatId = game.findPermanent("Blistering Firecat")!!
                val defenderId = game.findPermanent("Daunting Defender")!!
                val startingLife = game.getLifeTotal(2)

                // Declare attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(firecatId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Daunting Defender
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(defenderId to listOf(firecatId)))
                )

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Should have damage assignment decision
                withClue("Should have damage assignment decision") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision() shouldBe beInstanceOf<AssignDamageDecision>()
                }

                val decision = game.getPendingDecision() as AssignDamageDecision
                // Default accounts for prevention: 4 to blocker (3 toughness + 1 prevention), 3 to player
                withClue("Default should assign 4 to blocker (3 toughness + 1 prevention) and 3 to player") {
                    decision.defaultAssignments[defenderId] shouldBe 4
                    decision.defaultAssignments[game.player2Id] shouldBe 3
                }

                // Accept default assignment
                game.submitDefaultDamageAssignment()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Daunting Defender dies: 4 assigned - 1 prevented = 3 dealt = lethal for 3/3
                withClue("Daunting Defender should be dead (4 assigned - 1 prevented = 3 lethal)") {
                    game.findPermanent("Daunting Defender") shouldBe null
                    game.isInGraveyard(2, "Daunting Defender") shouldBe true
                }

                // Defending player takes 3 trample damage
                withClue("Defending player should take 3 trample damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 3
                }
            }

            test("Daunting Defender - player assigns less than prevention-aware default, Cleric survives") {
                // Blistering Firecat (7/1 trample) blocked by Daunting Defender (3/3 Cleric)
                // Player intentionally assigns only 3 (toughness, ignoring prevention) to maximize trample
                // 3 assigned - 1 prevented = 2 dealt < 3 toughness → Cleric survives
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Blistering Firecat")  // 7/1 trample haste
                    .withCardOnBattlefield(2, "Daunting Defender")   // 3/3 Human Cleric
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val firecatId = game.findPermanent("Blistering Firecat")!!
                val defenderId = game.findPermanent("Daunting Defender")!!
                val startingLife = game.getLifeTotal(2)

                // Declare attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(firecatId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Daunting Defender
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(defenderId to listOf(firecatId)))
                )

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Assign 3 to blocker (toughness without prevention), 4 to player
                game.submitDamageAssignment(
                    mapOf(defenderId to 3, game.player2Id to 4)
                )

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Daunting Defender survives: 3 assigned - 1 prevented = 2 dealt < 3 toughness
                withClue("Daunting Defender should survive (3 assigned - 1 prevented = 2 dealt)") {
                    game.findPermanent("Daunting Defender") shouldNotBe null
                }

                // Defending player takes 4 trample damage
                withClue("Defending player should take 4 trample damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 4
                }
            }

            test("non-trample creature blocked - no damage assignment decision") {
                // Glory Seeker (2/2, no trample) blocked by another creature
                // Should auto-assign damage with no decision
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3 no trample
                    .withCardOnBattlefield(2, "Glory Seeker")   // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val startingLife = game.getLifeTotal(2)

                // Declare attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(giantId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Glory Seeker
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(seekerId to listOf(giantId)))
                )

                // Advance all the way to postcombat - no decisions should be needed
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Glory Seeker should be dead (3 damage >= 2 toughness)
                withClue("Glory Seeker should be dead") {
                    game.findPermanent("Glory Seeker") shouldBe null
                }

                // No trample damage to player
                withClue("Defending player should take no damage (no trample)") {
                    game.getLifeTotal(2) shouldBe startingLife
                }

                // Hill Giant survives (2 damage < 3 toughness)
                withClue("Hill Giant should survive") {
                    game.findPermanent("Hill Giant") shouldNotBe null
                }
            }
        }
    }
}
