package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Defensive Maneuvers.
 *
 * Card reference:
 * - Defensive Maneuvers ({3}{W}): Instant
 *   "Creatures of the creature type of your choice get +0/+4 until end of turn."
 */
class DefensiveManeuversScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Defensive Maneuvers") {

            test("gives +0/+4 to all creatures of the chosen type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardOnBattlefield(1, "Wirewood Elf")     // 1/2 Elf Druid
                    .withCardOnBattlefield(2, "Wellwisher")       // 1/1 Elf
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 Human Soldier
                    .withCardInHand(1, "Defensive Maneuvers")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!
                val wirewoodElfId = game.findPermanent("Wirewood Elf")!!
                val wellwisherId = game.findPermanent("Wellwisher")!!
                val glorySeekerID = game.findPermanent("Glory Seeker")!!

                // Cast Defensive Maneuvers (no targets required)
                val result = game.castSpell(1, "Defensive Maneuvers")
                withClue("Cast should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Choose creature type during casting
                game.chooseCreatureType("Elf")

                // Resolve the spell
                game.resolveStack()

                // All Elves should get +0/+4
                val clientState = game.getClientState(1)

                val elvishWarriorInfo = clientState.cards[elvishWarriorId]
                withClue("Elvish Warrior (2/3 Elf) should be 2/7 after +0/+4") {
                    elvishWarriorInfo shouldNotBe null
                    elvishWarriorInfo!!.power shouldBe 2
                    elvishWarriorInfo.toughness shouldBe 7
                }

                val wirewoodElfInfo = clientState.cards[wirewoodElfId]
                withClue("Wirewood Elf (1/2 Elf) should be 1/6 after +0/+4") {
                    wirewoodElfInfo shouldNotBe null
                    wirewoodElfInfo!!.power shouldBe 1
                    wirewoodElfInfo.toughness shouldBe 6
                }

                // Opponent's Elf also gets the bonus
                val wellwisherInfo = clientState.cards[wellwisherId]
                withClue("Wellwisher (1/1 Elf) should be 1/5 after +0/+4") {
                    wellwisherInfo shouldNotBe null
                    wellwisherInfo!!.power shouldBe 1
                    wellwisherInfo.toughness shouldBe 5
                }

                // Non-Elf should NOT be affected
                val glorySeekerInfo = clientState.cards[glorySeekerID]
                withClue("Glory Seeker (2/2 Human) should remain 2/2") {
                    glorySeekerInfo shouldNotBe null
                    glorySeekerInfo!!.power shouldBe 2
                    glorySeekerInfo.toughness shouldBe 2
                }
            }

            test("buff survives through combat - buffed blockers should not die to lethal base-toughness damage") {
                // Reproduce bug: P1 has two 2/1 Goblin Bullies, casts Defensive Maneuvers
                // choosing Goblin (+0/+4 -> 2/5). P2 attacks with 4/3 Anurid Murkdiver.
                // Both goblins block. Neither should die (4 damage < 5 toughness).
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Goblin Bully")         // 2/1 Goblin
                    .withCardOnBattlefield(1, "Goblin Bully")         // 2/1 Goblin
                    .withCardOnBattlefield(2, "Anurid Murkdiver")     // 4/3 Zombie Frog Beast (swampwalk)
                    .withCardInHand(1, "Defensive Maneuvers")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val goblinIds = game.findAllPermanents("Goblin Bully")
                withClue("Should have 2 Goblin Bullies") { goblinIds.size shouldBe 2 }
                val murkdiverId = game.findPermanent("Anurid Murkdiver")!!

                // P2 declares Anurid Murkdiver as attacker (targeting P1)
                val attackResult = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(murkdiverId to game.player1Id))
                )
                withClue("Declare attackers: ${attackResult.error}") { attackResult.error shouldBe null }

                // P2 (active player) has priority after declaring attackers - pass to P1
                game.passPriority()

                // P1 casts Defensive Maneuvers (instant) during declare attackers step
                val castResult = game.castSpell(1, "Defensive Maneuvers")
                withClue("Cast should succeed: ${castResult.error}") { castResult.error shouldBe null }

                // Choose creature type during casting
                game.chooseCreatureType("Goblin")

                // Resolve the spell (both pass priority)
                game.resolveStack()

                // Verify the buff is applied (goblins should be 2/5)
                val clientState = game.getClientState(1)
                for (goblinId in goblinIds) {
                    val info = clientState.cards[goblinId]
                    withClue("Goblin Bully should be 2/5 after +0/+4") {
                        info shouldNotBe null
                        info!!.power shouldBe 2
                        info.toughness shouldBe 5
                    }
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Verify buff still active before blocking
                val preBlockState = game.getClientState(1)
                for (goblinId in goblinIds) {
                    val info = preBlockState.cards[goblinId]
                    withClue("Goblin Bully should still be 2/5 before blocking") {
                        info shouldNotBe null
                        info!!.toughness shouldBe 5
                    }
                }

                // P1 declares both Goblin Bullies as blockers for Anurid Murkdiver
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player1Id,
                        mapOf(
                            goblinIds[0] to listOf(murkdiverId),
                            goblinIds[1] to listOf(murkdiverId)
                        )
                    )
                )

                // Handle blocker order decision (attacker's controller orders blockers)
                if (game.state.pendingDecision is OrderObjectsDecision) {
                    val orderDecision = game.state.pendingDecision as OrderObjectsDecision
                    game.submitDecision(OrderedResponse(orderDecision.id, orderDecision.objects))
                }

                // Advance through combat damage
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                // Both Goblin Bullies should survive (4 damage distributed among 2/5 creatures)
                for (goblinId in goblinIds) {
                    withClue("Goblin Bully ($goblinId) should still be on the battlefield") {
                        game.isOnBattlefield("Goblin Bully") shouldBe true
                    }
                }
                withClue("Both Goblin Bullies should still be on the battlefield") {
                    game.findAllPermanents("Goblin Bully").size shouldBe 2
                }

                // Anurid Murkdiver took 4 damage (2+2) vs 3 toughness - should be dead
                withClue("Anurid Murkdiver should be in graveyard") {
                    game.isInGraveyard(2, "Anurid Murkdiver") shouldBe true
                }
            }

            test("buff expires at end of turn - goblins die next turn without buff") {
                // If P1 casts DM on their OWN turn, the buff expires at end of that turn.
                // On P2's turn, the goblins are back to 2/1 and die to combat damage.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Bully")
                    .withCardOnBattlefield(1, "Goblin Bully")
                    .withCardOnBattlefield(2, "Anurid Murkdiver")     // 4/3
                    .withCardInHand(1, "Defensive Maneuvers")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    // Add library cards so players don't lose from drawing an empty library
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // P1 casts DM during their own main phase
                game.castSpell(1, "Defensive Maneuvers")
                game.chooseCreatureType("Goblin")
                game.resolveStack()

                // Verify buff is active during P1's turn
                val goblinIds = game.findAllPermanents("Goblin Bully")
                for (goblinId in goblinIds) {
                    val info = game.getClientState(1).cards[goblinId]
                    withClue("Goblin should be 2/5 during P1's turn") {
                        info!!.toughness shouldBe 5
                    }
                }

                // Advance past P1's turn to P2's precombat main
                // First advance to postcombat main (past P1's combat)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                // Then cross the turn boundary to P2's precombat main
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Verify it's now P2's turn
                withClue("Active player should now be P2") {
                    game.state.activePlayerId shouldBe game.player2Id
                }

                // Verify buff has expired (goblins should be 2/1 again)
                for (goblinId in goblinIds) {
                    val info = game.getClientState(1).cards[goblinId]
                    withClue("Goblin should be 2/1 after P1's turn ends") {
                        info!!.toughness shouldBe 1
                    }
                }

                // Advance to P2's declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // P2 attacks with Anurid Murkdiver
                val murkdiverId = game.findPermanent("Anurid Murkdiver")!!
                val attackResult = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(murkdiverId to game.player1Id))
                )
                withClue("Declare attackers: ${attackResult.error}") { attackResult.error shouldBe null }

                // Pass to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // P1 blocks with both goblins
                val currentGoblinIds = game.findAllPermanents("Goblin Bully")
                game.execute(
                    DeclareBlockers(
                        game.player1Id,
                        mapOf(
                            currentGoblinIds[0] to listOf(murkdiverId),
                            currentGoblinIds[1] to listOf(murkdiverId)
                        )
                    )
                )

                // Handle blocker order
                if (game.state.pendingDecision is OrderObjectsDecision) {
                    val orderDecision = game.state.pendingDecision as OrderObjectsDecision
                    game.submitDecision(OrderedResponse(orderDecision.id, orderDecision.objects))
                }

                // Advance through combat damage
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                // Both goblins should be dead (2/1 without buff, 4/3 attacker deals enough)
                withClue("Both goblins should die without the buff") {
                    game.findAllPermanents("Goblin Bully").size shouldBe 0
                }

                // Anurid Murkdiver took 4 damage (2+2) vs 3 toughness - should also be dead
                withClue("Anurid Murkdiver should be in graveyard") {
                    game.isInGraveyard(2, "Anurid Murkdiver") shouldBe true
                }
            }

            test("does nothing if no creatures of the chosen type exist") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 Human Soldier
                    .withCardInHand(1, "Defensive Maneuvers")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeekerID = game.findPermanent("Glory Seeker")!!

                game.castSpell(1, "Defensive Maneuvers")
                game.chooseCreatureType("Dragon")
                game.resolveStack()

                // Glory Seeker should be unaffected
                val clientState = game.getClientState(1)
                val glorySeekerInfo = clientState.cards[glorySeekerID]
                withClue("Glory Seeker should remain 2/2") {
                    glorySeekerInfo shouldNotBe null
                    glorySeekerInfo!!.power shouldBe 2
                    glorySeekerInfo.toughness shouldBe 2
                }
            }
        }
    }
}
