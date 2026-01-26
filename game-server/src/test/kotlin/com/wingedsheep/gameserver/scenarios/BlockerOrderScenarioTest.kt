package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for blocker damage assignment order.
 *
 * Per MTG CR 509.2: After blockers are declared, the attacking player must
 * declare the damage assignment order for each attacking creature that is
 * blocked by two or more creatures.
 *
 * These tests verify:
 * 1. The game pauses for blocker order decision when multiple blockers exist
 * 2. The player can submit their preferred order
 * 3. Combat damage respects the declared order
 */
class BlockerOrderScenarioTest : ScenarioTestBase() {

    init {
        context("Single attacker with multiple blockers") {
            test("game pauses for blocker order when attacker is blocked by 2+ creatures") {
                // Setup:
                // - Player 1 has a big creature (Hill Giant 3/3) that will attack
                // - Player 2 has two smaller blockers (Grizzly Bears 2/2 each)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")      // 3/3 attacker
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2 blocker 1
                    .withCardOnBattlefield(2, "Devoted Hero")    // 2/1 blocker 2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val bears = game.findAllPermanents("Grizzly Bears")
                val devotedHero = game.findPermanent("Devoted Hero")!!

                // Declare Hill Giant as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                withClue("Attackers should be declared successfully: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declare both creatures as blockers for the Hill Giant
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            bears[0] to listOf(hillGiantId),
                            devotedHero to listOf(hillGiantId)
                        )
                    )
                )

                withClue("Declare blockers result: ${blockResult.error}") {
                    // Should be paused, not success, because we need blocker order
                    blockResult.pendingDecision shouldNotBe null
                }

                // Verify it's an OrderObjectsDecision for the attacking player
                val decision = game.state.pendingDecision
                withClue("Should have pending decision for blocker order") {
                    decision shouldNotBe null
                    decision.shouldBeInstanceOf<OrderObjectsDecision>()
                }

                val orderDecision = decision as OrderObjectsDecision
                withClue("Decision should be for the attacking player") {
                    orderDecision.playerId shouldBe game.player1Id
                }
                withClue("Decision should contain both blockers") {
                    orderDecision.objects.size shouldBe 2
                    orderDecision.objects.toSet() shouldBe setOf(bears[0], devotedHero)
                }
            }

            test("submitting blocker order stores the order on the attacker") {
                // Same setup as above
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val bears = game.findAllPermanents("Grizzly Bears")
                val devotedHero = game.findPermanent("Devoted Hero")!!

                // Declare attack
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declare both blockers
                game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            bears[0] to listOf(hillGiantId),
                            devotedHero to listOf(hillGiantId)
                        )
                    )
                )

                // Get the decision and submit order: Devoted Hero first, then Grizzly Bears
                val decision = game.state.pendingDecision as OrderObjectsDecision
                val orderResult = game.submitDecision(
                    OrderedResponse(decision.id, listOf(devotedHero, bears[0]))
                )

                withClue("Order submission should succeed: ${orderResult.error}") {
                    orderResult.error shouldBe null
                }

                // Verify the DamageAssignmentOrderComponent was added to the attacker
                val attackerEntity = game.state.getEntity(hillGiantId)
                withClue("Attacker should have DamageAssignmentOrderComponent") {
                    attackerEntity shouldNotBe null
                    val orderComponent = attackerEntity!!.get<DamageAssignmentOrderComponent>()
                    orderComponent shouldNotBe null
                    orderComponent!!.orderedBlockers shouldContainExactly listOf(devotedHero, bears[0])
                }

                // No more pending decision after submitting order
                withClue("Should have no more pending decision") {
                    game.state.pendingDecision shouldBe null
                }
            }

            test("single blocker does not require order declaration") {
                // Setup with only one blocker
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val bears = game.findAllPermanents("Grizzly Bears")

                // Declare attack
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declare single blocker
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(bears[0] to listOf(hillGiantId))
                    )
                )

                withClue("Single blocker should not require order decision: ${blockResult.error}") {
                    blockResult.error shouldBe null
                    blockResult.pendingDecision shouldBe null
                }
            }
        }

        context("Multiple attackers with multiple blockers each") {
            test("game asks for order on each attacker separately") {
                // Setup:
                // - Player 1 has two attackers
                // - Player 2 has four blockers (2 for each attacker)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")      // First attacker
                    .withCardOnBattlefield(1, "Grizzly Bears")   // Second attacker
                    .withCardOnBattlefield(2, "Devoted Hero")    // Blocker 1
                    .withCardOnBattlefield(2, "Devoted Hero")    // Blocker 2
                    .withCardOnBattlefield(2, "Devoted Hero")    // Blocker 3
                    .withCardOnBattlefield(2, "Devoted Hero")    // Blocker 4
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val attackerBears = game.findAllPermanents("Grizzly Bears").first {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId == game.player1Id
                }
                val blockers = game.findAllPermanents("Devoted Hero")

                // Declare both creatures attacking
                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(
                            hillGiantId to game.player2Id,
                            attackerBears to game.player2Id
                        )
                    )
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block Hill Giant with blockers 0 and 1, Grizzly Bears with blockers 2 and 3
                game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            blockers[0] to listOf(hillGiantId),
                            blockers[1] to listOf(hillGiantId),
                            blockers[2] to listOf(attackerBears),
                            blockers[3] to listOf(attackerBears)
                        )
                    )
                )

                // First decision - should be for one of the attackers
                val decision1 = game.state.pendingDecision
                withClue("Should have first blocker order decision") {
                    decision1 shouldNotBe null
                    decision1.shouldBeInstanceOf<OrderObjectsDecision>()
                }

                val orderDecision1 = decision1 as OrderObjectsDecision

                // Submit first order
                game.submitDecision(
                    OrderedResponse(orderDecision1.id, orderDecision1.objects)
                )

                // Second decision - should be for the other attacker
                val decision2 = game.state.pendingDecision
                withClue("Should have second blocker order decision") {
                    decision2 shouldNotBe null
                    decision2.shouldBeInstanceOf<OrderObjectsDecision>()
                }

                val orderDecision2 = decision2 as OrderObjectsDecision

                // Submit second order
                game.submitDecision(
                    OrderedResponse(orderDecision2.id, orderDecision2.objects)
                )

                // Now both orders should be set
                withClue("Should have no more pending decisions") {
                    game.state.pendingDecision shouldBe null
                }

                // Both attackers should have order components
                val hillGiantOrder = game.state.getEntity(hillGiantId)?.get<DamageAssignmentOrderComponent>()
                val bearsOrder = game.state.getEntity(attackerBears)?.get<DamageAssignmentOrderComponent>()

                withClue("Hill Giant should have blocker order") {
                    hillGiantOrder shouldNotBe null
                    hillGiantOrder!!.orderedBlockers.size shouldBe 2
                }

                withClue("Grizzly Bears should have blocker order") {
                    bearsOrder shouldNotBe null
                    bearsOrder!!.orderedBlockers.size shouldBe 2
                }
            }
        }

        context("Priority restoration after blocker ordering") {
            test("active player gets priority after blocker order is submitted") {
                // Setup
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val bears = game.findAllPermanents("Grizzly Bears")
                val devotedHero = game.findPermanent("Devoted Hero")!!

                // Declare attack
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declare both blockers
                game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            bears[0] to listOf(hillGiantId),
                            devotedHero to listOf(hillGiantId)
                        )
                    )
                )

                // Submit the blocker order
                val decision = game.state.pendingDecision as OrderObjectsDecision
                game.submitDecision(
                    OrderedResponse(decision.id, listOf(devotedHero, bears[0]))
                )

                // After blocker ordering completes, the active player (attacker) should have priority
                withClue("Active player should have priority after blocker ordering") {
                    game.state.priorityPlayerId shouldBe game.player1Id
                }

                // Should still be in declare blockers step
                withClue("Should still be in declare blockers step") {
                    game.state.step shouldBe Step.DECLARE_BLOCKERS
                }

                // No pending decision
                withClue("Should have no pending decision") {
                    game.state.pendingDecision shouldBe null
                }
            }

            test("combat damage respects declared blocker order") {
                // Setup: Hill Giant (3/3) vs two blockers
                // If ordered: Devoted Hero (2/1) first, Grizzly Bears (2/2) second
                // Hill Giant deals 3 damage: 1 to kill Devoted Hero, 2 to Grizzly Bears (survives)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val bears = game.findAllPermanents("Grizzly Bears")
                val devotedHero = game.findPermanent("Devoted Hero")!!

                // Declare attack
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declare both blockers
                game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            bears[0] to listOf(hillGiantId),
                            devotedHero to listOf(hillGiantId)
                        )
                    )
                )

                // Order: Devoted Hero first, then Grizzly Bears
                // Hill Giant (3/3) should deal 1 damage to Devoted Hero (lethal), then 2 to Grizzly Bears
                val decision = game.state.pendingDecision as OrderObjectsDecision
                game.submitDecision(
                    OrderedResponse(decision.id, listOf(devotedHero, bears[0]))
                )

                // Verify we're in declare blockers with priority
                withClue("Should be in declare blockers step") {
                    game.state.step shouldBe Step.DECLARE_BLOCKERS
                }
                withClue("Active player should have priority") {
                    game.state.priorityPlayerId shouldBe game.player1Id
                }

                // Pass priority to advance to first strike combat damage (skipped if no first strikers)
                // then to regular combat damage step
                game.execute(PassPriority(game.player1Id))  // Attacker passes
                game.execute(PassPriority(game.player2Id))  // Defender passes - advances to first strike damage

                // Skip through first strike damage step (no first strikers)
                if (game.state.step == Step.FIRST_STRIKE_COMBAT_DAMAGE) {
                    game.execute(PassPriority(game.player1Id))
                    game.execute(PassPriority(game.player2Id))
                }

                // Now we should be in combat damage step
                withClue("Should be in combat damage step") {
                    game.state.step shouldBe Step.COMBAT_DAMAGE
                }

                // Devoted Hero (2/1) should be dead (took 1 lethal from Hill Giant)
                withClue("Devoted Hero should be in graveyard (took lethal damage first)") {
                    game.findPermanent("Devoted Hero") shouldBe null
                }

                // Grizzly Bears (2/2) should still be alive (only took 2 damage)
                withClue("Grizzly Bears should still be alive (only took 2 damage)") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 1
                }

                // Hill Giant (3/3) should be dead (took 4 damage from 2/2 + 2/1)
                withClue("Hill Giant should be in graveyard (took 4 damage from blockers)") {
                    game.findPermanent("Hill Giant") shouldBe null
                }
            }
        }
    }
}
