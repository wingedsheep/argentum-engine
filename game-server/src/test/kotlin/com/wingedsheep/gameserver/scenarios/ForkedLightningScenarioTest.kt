package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Forked Lightning.
 *
 * Card: Forked Lightning
 * {3}{R}
 * Sorcery
 * "Forked Lightning deals 4 damage divided as you choose among one, two,
 * or three target creatures."
 *
 * Test scenarios:
 * 1. Single target: All 4 damage goes to one creature
 * 2. Two targets: Player distributes damage (e.g., 2+2 or 3+1)
 * 3. Three targets: Player distributes damage (e.g., 2+1+1)
 */
class ForkedLightningScenarioTest : ScenarioTestBase() {

    init {
        context("Forked Lightning basic functionality") {
            test("deals all 4 damage to single target directly without distribution prompt") {
                // Setup: Player 1 has Forked Lightning in hand with Mountains
                // Player 2 has a single creature on the battlefield
                val game = scenario()
                    .withPlayers("Caster", "Target")
                    .withCardInHand(1, "Forked Lightning")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Record initial state
                val hillGiant = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant should be on the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }

                // Cast Forked Lightning targeting Hill Giant
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Forked Lightning").first(),
                        listOf(ChosenTarget.Permanent(hillGiant))
                    )
                )
                withClue("Forked Lightning should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // With single target, damage should be dealt directly without a distribution decision
                withClue("No pending decision for single target") {
                    game.hasPendingDecision() shouldBe false
                }

                // Hill Giant (3/3) takes 4 damage and dies
                withClue("Hill Giant should be destroyed (took 4 damage)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("prompts for damage distribution with two targets") {
                // Setup: Player 1 has Forked Lightning
                // Player 2 has two creatures
                val game = scenario()
                    .withPlayers("Caster", "Target")
                    .withCardInHand(1, "Forked Lightning")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Hill Giant")       // 3/3
                    .withCardOnBattlefield(2, "Raging Goblin")    // 1/1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiant = game.findPermanent("Hill Giant")!!
                val ragingGoblin = game.findPermanent("Raging Goblin")!!

                // Cast Forked Lightning targeting both creatures
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Forked Lightning").first(),
                        listOf(
                            ChosenTarget.Permanent(hillGiant),
                            ChosenTarget.Permanent(ragingGoblin)
                        )
                    )
                )
                withClue("Forked Lightning should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Should prompt for damage distribution
                withClue("Should have a pending distribution decision") {
                    game.hasPendingDecision() shouldBe true
                }

                val decision = game.getPendingDecision()
                withClue("Decision should be DistributeDecision") {
                    decision.shouldBeInstanceOf<DistributeDecision>()
                }

                val distributeDecision = decision as DistributeDecision
                withClue("Total damage should be 4") {
                    distributeDecision.totalAmount shouldBe 4
                }
                withClue("Should have 2 targets") {
                    distributeDecision.targets.size shouldBe 2
                }
                withClue("Minimum damage per target should be 1") {
                    distributeDecision.minPerTarget shouldBe 1
                }

                // Distribute 3 damage to Hill Giant, 1 to Raging Goblin
                val distributionResult = game.submitDistribution(
                    mapOf(
                        hillGiant to 3,
                        ragingGoblin to 1
                    )
                )
                withClue("Distribution should succeed: ${distributionResult.error}") {
                    distributionResult.error shouldBe null
                }

                // Hill Giant dies (3 damage, 3 toughness), Raging Goblin dies (1 damage, 1 toughness)
                // SBAs are now checked automatically after submitDistribution
                withClue("Hill Giant should be destroyed (took lethal damage)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Raging Goblin should be destroyed (took lethal damage)") {
                    game.isOnBattlefield("Raging Goblin") shouldBe false
                }
            }

            test("distributes damage among three targets") {
                // Setup: Player 1 has Forked Lightning
                // Player 2 has three creatures
                val game = scenario()
                    .withPlayers("Caster", "Target")
                    .withCardInHand(1, "Forked Lightning")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Hill Giant")       // 3/3
                    .withCardOnBattlefield(2, "Raging Goblin")    // 1/1
                    .withCardOnBattlefield(2, "Elvish Ranger")    // 4/1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiant = game.findPermanent("Hill Giant")!!
                val ragingGoblin = game.findPermanent("Raging Goblin")!!
                val elvishRanger = game.findPermanent("Elvish Ranger")!!

                // Cast Forked Lightning targeting all three creatures
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Forked Lightning").first(),
                        listOf(
                            ChosenTarget.Permanent(hillGiant),
                            ChosenTarget.Permanent(ragingGoblin),
                            ChosenTarget.Permanent(elvishRanger)
                        )
                    )
                )
                withClue("Forked Lightning should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Should prompt for damage distribution
                val decision = game.getPendingDecision() as DistributeDecision
                withClue("Should have 3 targets") {
                    decision.targets.size shouldBe 3
                }

                // Distribute 2 to Hill Giant, 1 to Raging Goblin, 1 to Elvish Ranger
                val distributionResult = game.submitDistribution(
                    mapOf(
                        hillGiant to 2,
                        ragingGoblin to 1,
                        elvishRanger to 1
                    )
                )
                withClue("Distribution should succeed: ${distributionResult.error}") {
                    distributionResult.error shouldBe null
                }

                // Hill Giant survives (2 damage, 3 toughness)
                // SBAs are now checked automatically after submitDistribution
                // Raging Goblin dies (1 damage, 1 toughness)
                // Elvish Ranger dies (1 damage, 1 toughness)
                withClue("Hill Giant should survive (only took 2 damage)") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Raging Goblin should be destroyed") {
                    game.isOnBattlefield("Raging Goblin") shouldBe false
                    game.isInGraveyard(2, "Raging Goblin") shouldBe true
                }
                withClue("Elvish Ranger should be destroyed") {
                    game.isOnBattlefield("Elvish Ranger") shouldBe false
                    game.isInGraveyard(2, "Elvish Ranger") shouldBe true
                }
            }
        }
    }
}
