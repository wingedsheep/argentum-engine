package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Scenario tests for Whipgrass Entangler's activated ability.
 *
 * Whipgrass Entangler: {2}{W} 1/3 Creature — Human Cleric
 * {1}{W}: Until end of turn, target creature gains "This creature can't attack or block unless
 * its controller pays {1} for each Cleric on the battlefield."
 */
class WhipgrassEntanglerScenarioTest : ScenarioTestBase() {

    init {
        context("Whipgrass Entangler attack tax") {

            test("targeted creature cannot attack when controller cannot pay tax") {
                // P2 is the active player (attacker). P1 controls Whipgrass Entangler.
                // Activate during P2's turn so the "until end of turn" effect is active during combat.
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Whipgrass Entangler")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    // Attacker has no mana to pay the tax
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val entanglerId = game.findPermanent("Whipgrass Entangler")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val cardDef = cardRegistry.getCard("Whipgrass Entangler")!!
                val ability = cardDef.script.activatedAbilities.first()

                // P2 (active) passes priority to P1
                game.execute(PassPriority(game.player2Id))

                // P1 activates Whipgrass Entangler targeting Hill Giant
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = entanglerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(hillGiantId))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Advance to declare attackers step
                advanceToCombat(game)

                // P2 tries to attack with Hill Giant but has no mana (1 Cleric = costs {1})
                // The engine pauses to confirm before tapping mana; on yes, payment fails.
                val declared = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(hillGiantId to game.player1Id))
                )
                declared.isPaused shouldBe true

                val paid = game.submitManaSourcesAutoPay()
                withClue("Attack should fail - no mana to pay tax") {
                    paid.error shouldNotBe null
                    paid.error!! shouldContainIgnoringCase "tax"
                }
            }

            test("targeted creature can attack when controller can pay tax") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Whipgrass Entangler")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Mountain", 1) // Enough for {1} tax with 1 Cleric
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val entanglerId = game.findPermanent("Whipgrass Entangler")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val cardDef = cardRegistry.getCard("Whipgrass Entangler")!!
                val ability = cardDef.script.activatedAbilities.first()

                // P2 passes priority, P1 activates ability
                game.execute(PassPriority(game.player2Id))
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = entanglerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(hillGiantId))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()
                advanceToCombat(game)

                // P2 has 1 Mountain, 1 Cleric on battlefield = tax is {1}
                val declared = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(hillGiantId to game.player1Id))
                )
                declared.isPaused shouldBe true

                val paid = game.submitManaSourcesAutoPay()
                withClue("Attack should succeed - can pay {1} tax: ${paid.error}") {
                    paid.error shouldBe null
                }
            }

            test("tax scales with number of Clerics on the battlefield") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Whipgrass Entangler")
                    .withCardOnBattlefield(1, "Akroma's Devoted") // Another Cleric
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Mountain", 1) // Only 1 mana, but needs {2} (2 Clerics)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val entanglerId = game.findPermanent("Whipgrass Entangler")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val cardDef = cardRegistry.getCard("Whipgrass Entangler")!!
                val ability = cardDef.script.activatedAbilities.first()

                // P2 passes, P1 activates
                game.execute(PassPriority(game.player2Id))
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = entanglerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(hillGiantId))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()
                advanceToCombat(game)

                // P2 has 1 Mountain but needs {2} (2 Clerics: Whipgrass Entangler + Akroma's Devoted)
                val declared = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(hillGiantId to game.player1Id))
                )
                declared.isPaused shouldBe true

                val paid = game.submitManaSourcesAutoPay()
                withClue("Attack should fail - only 1 mana for 2 Clerics (needs {2})") {
                    paid.error shouldNotBe null
                    paid.error!! shouldContainIgnoringCase "tax"
                }
            }

            test("no tax when ability has not been activated") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Whipgrass Entangler")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(hillGiantId to game.player1Id))
                )

                withClue("Attack should succeed - no activation: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }
        }

        context("Whipgrass Entangler block tax") {

            test("targeted creature cannot block when controller cannot pay tax") {
                // P1 is the attacker (active player). P1 also controls Whipgrass Entangler.
                // P2 has a blocker creature. P1 activates ability targeting P2's creature.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Whipgrass Entangler")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Glory Seeker")
                    // P2 has no mana to pay block tax
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val entanglerId = game.findPermanent("Whipgrass Entangler")!!
                val glorySeekerIdOnP2 = game.findPermanent("Glory Seeker")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val cardDef = cardRegistry.getCard("Whipgrass Entangler")!!
                val ability = cardDef.script.activatedAbilities.first()

                // P1 activates targeting P2's Glory Seeker
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = entanglerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(glorySeekerIdOnP2))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()
                advanceToCombat(game)

                // P1 attacks with Hill Giant
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers (pass priority through declare attackers step)
                game.passPriority() // P1 passes
                game.passPriority() // P2 passes → moves to declare blockers

                // P2 tries to block with Glory Seeker but has no mana (1 Cleric = costs {1})
                // Block tax now pauses for confirmation too.
                val declaredBlock = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(glorySeekerIdOnP2 to listOf(hillGiantId)))
                )
                declaredBlock.isPaused shouldBe true

                val paidBlock = game.submitManaSourcesAutoPay()
                withClue("Block should fail - no mana to pay tax") {
                    paidBlock.error shouldNotBe null
                    paidBlock.error!! shouldContainIgnoringCase "tax"
                }
            }
        }
    }

    /**
     * Advance from the current phase to declare attackers step.
     */
    private fun advanceToCombat(game: TestGame) {
        var iterations = 0
        while ((game.state.phase != Phase.COMBAT || game.state.step != Step.DECLARE_ATTACKERS) && iterations++ < 50) {
            val playerId = game.state.priorityPlayerId ?: break
            game.execute(PassPriority(playerId))
        }
    }
}
