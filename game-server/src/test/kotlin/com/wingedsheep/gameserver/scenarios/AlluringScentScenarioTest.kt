package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for Alluring Scent's blocking requirement effect.
 *
 * Card reference:
 * - Alluring Scent (1GG): "All creatures able to block target creature this turn do so."
 *
 * These tests verify that the "must be blocked by all" requirement is properly enforced.
 */
class AlluringScentScenarioTest : ScenarioTestBase() {

    init {
        context("Alluring Scent single target") {
            test("all creatures that can block must block the targeted creature") {
                // Setup:
                // - Player 1 has a creature (Grizzly Bears) that will attack
                // - Player 1 has mana to cast Alluring Scent
                // - Player 2 has two potential blockers (elite vanguards)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 attacker
                    .withCardOnBattlefield(2, "Devoted Hero") // 2/1 blocker
                    .withCardOnBattlefield(2, "Devoted Hero") // 2/1 blocker
                    .withCardInHand(1, "Alluring Scent")
                    .withLandsOnBattlefield(1, "Forest", 3)     // Enough mana for {1}{G}{G}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzlyId = game.findPermanent("Grizzly Bears")!!
                val vanguards = game.findAllPermanents("Devoted Hero")
                vanguards.size shouldBe 2

                // Cast Alluring Scent targeting Grizzly Bears
                val castResult = game.castSpell(1, "Alluring Scent", grizzlyId)
                withClue("Alluring Scent should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Advance to declare attackers step
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Declare Grizzly Bears as attacker
                val attackResult = game.declareAttackers(mapOf("Grizzly Bears" to 2))
                withClue("Attackers should be declared successfully: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers step
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to declare no blockers - this should FAIL
                val noBlockResult = game.declareNoBlockers()
                withClue("Declaring no blockers should fail when creatures must block") {
                    noBlockResult.error shouldNotBe null
                    noBlockResult.error!! shouldContain "must block"
                }

                // Try to declare only one blocker - this should also FAIL
                val oneBlockerResult = game.declareBlockers(mapOf(
                    "Devoted Hero" to listOf("Grizzly Bears")
                ))
                withClue("Declaring only one blocker should fail when both must block") {
                    oneBlockerResult.error shouldNotBe null
                    oneBlockerResult.error!! shouldContain "must block"
                }
            }

            test("both blockers blocking the targeted creature is valid") {
                // Same setup as above
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withCardInHand(1, "Alluring Scent")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzlyId = game.findPermanent("Grizzly Bears")!!

                // Cast and resolve Alluring Scent
                game.castSpell(1, "Alluring Scent", grizzlyId)
                game.resolveStack()

                // Advance to combat
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2))
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declare BOTH vanguards as blockers - this should succeed
                // We need to use the entity IDs since both have the same name
                val vanguardIds = game.findAllPermanents("Devoted Hero")
                val grizzlyIdForBlock = game.findPermanent("Grizzly Bears")!!

                val blockResult = game.execute(
                    com.wingedsheep.engine.core.DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            vanguardIds[0] to listOf(grizzlyIdForBlock),
                            vanguardIds[1] to listOf(grizzlyIdForBlock)
                        )
                    )
                )

                withClue("Both creatures blocking should be valid: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }

        context("Alluring Scent on multiple creatures") {
            test("when two creatures have must-be-blocked, each blocker must block one of them") {
                // Setup:
                // - Player 1 has two attackers: Grizzly Bears and another creature
                // - Player 1 casts Alluring Scent on both
                // - Player 2 has one blocker
                // - The blocker must block ONE of the must-be-blocked creatures (player's choice)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")   // First attacker
                    .withCardOnBattlefield(1, "Devoted Hero")  // Second attacker
                    .withCardOnBattlefield(2, "Hill Giant")      // 3/3 blocker
                    .withCardInHand(1, "Alluring Scent")
                    .withCardInHand(1, "Alluring Scent")         // Two copies
                    .withLandsOnBattlefield(1, "Forest", 6)      // Enough mana for both
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzlyId = game.findPermanent("Grizzly Bears")!!
                val vanguardId = game.findPermanent("Devoted Hero")!!

                // Cast Alluring Scent on Grizzly Bears and resolve
                game.castSpell(1, "Alluring Scent", grizzlyId)
                game.resolveStack()

                // Cast Alluring Scent on Devoted Hero and resolve
                game.castSpell(1, "Alluring Scent", vanguardId)
                game.resolveStack()

                // Advance to combat
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Declare both as attackers
                val attackResult = game.execute(
                    com.wingedsheep.engine.core.DeclareAttackers(
                        game.player1Id,
                        mapOf(
                            grizzlyId to game.player2Id,
                            vanguardId to game.player2Id
                        )
                    )
                )
                withClue("Both attackers should be declared: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to declare no blockers - this should FAIL
                val noBlockResult = game.declareNoBlockers()
                withClue("Declaring no blockers should fail") {
                    noBlockResult.error shouldNotBe null
                    noBlockResult.error!! shouldContain "must block"
                }

                // Block Grizzly Bears only - this should SUCCEED
                // (Hill Giant can only block one, and it's blocking one of the must-be-blocked creatures)
                val blockGrizzlyResult = game.declareBlockers(mapOf(
                    "Hill Giant" to listOf("Grizzly Bears")
                ))
                withClue("Blocking one of the must-be-blocked creatures should succeed: ${blockGrizzlyResult.error}") {
                    blockGrizzlyResult.error shouldBe null
                }
            }

            test("with two blockers and two must-be-blocked attackers, each blocker must block one") {
                // Setup:
                // - Player 1 has two attackers both with Alluring Scent
                // - Player 2 has two blockers
                // - Each blocker must block one of the must-be-blocked attackers
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")   // First attacker
                    .withCardOnBattlefield(1, "Hill Giant")      // Second attacker
                    .withCardOnBattlefield(2, "Devoted Hero")  // First blocker
                    .withCardOnBattlefield(2, "Primeval Force")  // Second blocker (8/8)
                    .withCardInHand(1, "Alluring Scent")
                    .withCardInHand(1, "Alluring Scent")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzlyId = game.findPermanent("Grizzly Bears")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!

                // Cast Alluring Scent on both attackers
                game.castSpell(1, "Alluring Scent", grizzlyId)
                game.resolveStack()
                game.castSpell(1, "Alluring Scent", hillGiantId)
                game.resolveStack()

                // Advance to combat
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.execute(
                    com.wingedsheep.engine.core.DeclareAttackers(
                        game.player1Id,
                        mapOf(
                            grizzlyId to game.player2Id,
                            hillGiantId to game.player2Id
                        )
                    )
                )

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Both blockers blocking only one attacker - should FAIL
                // (The other blocker isn't blocking anything)
                val vanguardId = game.findPermanent("Devoted Hero")!!
                val forceId = game.findPermanent("Primeval Force")!!

                val oneBlockerOnlyResult = game.execute(
                    com.wingedsheep.engine.core.DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            vanguardId to listOf(grizzlyId)
                            // Primeval Force not blocking - should fail!
                        )
                    )
                )
                withClue("Having one blocker not blocking should fail") {
                    oneBlockerOnlyResult.error shouldNotBe null
                    oneBlockerOnlyResult.error!! shouldContain "must block"
                }

                // Both blockers each blocking one attacker - should SUCCEED
                val bothBlockingResult = game.execute(
                    com.wingedsheep.engine.core.DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            vanguardId to listOf(grizzlyId),
                            forceId to listOf(hillGiantId)
                        )
                    )
                )
                withClue("Both blockers each blocking one should succeed: ${bothBlockingResult.error}") {
                    bothBlockingResult.error shouldBe null
                }
            }
        }

        context("Alluring Scent with evasion") {
            test("creatures that cannot block due to flying are not required to block") {
                // Setup:
                // - Player 1 has a flying creature with Alluring Scent
                // - Player 2 has a non-flying, non-reach creature
                // - The blocker should NOT be required to block (it can't!)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cloud Spirit")    // 3/1 Flying
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2 no flying/reach
                    .withCardInHand(1, "Alluring Scent")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cloudSpiritId = game.findPermanent("Cloud Spirit")!!

                // Cast Alluring Scent on Cloud Spirit
                game.castSpell(1, "Alluring Scent", cloudSpiritId)
                game.resolveStack()

                // Advance to combat
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Cloud Spirit" to 2))
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declare no blockers - should SUCCEED because Grizzly Bears can't block flying
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should be valid when blocker can't reach flyer: ${noBlockResult.error}") {
                    noBlockResult.error shouldBe null
                }
            }
        }
    }
}
