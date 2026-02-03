package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Forked Lightning.
 *
 * Card: Forked Lightning
 * {3}{R}
 * Sorcery
 * "Forked Lightning deals 4 damage divided as you choose among one, two,
 * or three target creatures."
 *
 * Per MTG rules, damage distribution for "divided as you choose" effects
 * must be chosen as part of targeting—at cast time—not when the spell resolves.
 *
 * Test scenarios:
 * 1. Single target: All 4 damage goes to one creature (no distribution needed)
 * 2. Two targets: Distribution provided at cast time
 * 3. Three targets: Distribution provided at cast time
 */
class ForkedLightningScenarioTest : ScenarioTestBase() {

    init {
        context("Forked Lightning basic functionality") {
            test("deals all 4 damage to single target directly without distribution") {
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

                // Cast Forked Lightning targeting Hill Giant (single target - no distribution needed)
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Forked Lightning").first(),
                        listOf(ChosenTarget.Permanent(hillGiant))
                        // No damageDistribution needed for single target
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

            test("deals damage distributed at cast time with two targets") {
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

                // Cast Forked Lightning targeting both creatures with damage distribution
                // Distribution: 3 damage to Hill Giant, 1 to Raging Goblin
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Forked Lightning").first(),
                        targets = listOf(
                            ChosenTarget.Permanent(hillGiant),
                            ChosenTarget.Permanent(ragingGoblin)
                        ),
                        damageDistribution = mapOf(
                            hillGiant to 3,
                            ragingGoblin to 1
                        )
                    )
                )
                withClue("Forked Lightning should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell - damage distribution was already chosen at cast time
                game.resolveStack()

                // Should NOT have a pending decision - distribution was provided at cast time
                withClue("No pending decision - distribution was chosen at cast time") {
                    game.hasPendingDecision() shouldBe false
                }

                // Hill Giant dies (3 damage, 3 toughness), Raging Goblin dies (1 damage, 1 toughness)
                withClue("Hill Giant should be destroyed (took lethal damage)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Raging Goblin should be destroyed (took lethal damage)") {
                    game.isOnBattlefield("Raging Goblin") shouldBe false
                }
            }

            test("distributes damage among three targets at cast time") {
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

                // Cast Forked Lightning targeting all three creatures with damage distribution
                // Distribution: 2 damage to Hill Giant, 1 to Raging Goblin, 1 to Elvish Ranger
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Forked Lightning").first(),
                        targets = listOf(
                            ChosenTarget.Permanent(hillGiant),
                            ChosenTarget.Permanent(ragingGoblin),
                            ChosenTarget.Permanent(elvishRanger)
                        ),
                        damageDistribution = mapOf(
                            hillGiant to 2,
                            ragingGoblin to 1,
                            elvishRanger to 1
                        )
                    )
                )
                withClue("Forked Lightning should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell - damage distribution was already chosen at cast time
                game.resolveStack()

                // Should NOT have a pending decision - distribution was provided at cast time
                withClue("No pending decision - distribution was chosen at cast time") {
                    game.hasPendingDecision() shouldBe false
                }

                // Hill Giant survives (2 damage, 3 toughness)
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
