package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Deep Wood's damage prevention effect.
 *
 * Card reference:
 * - Deep Wood ({1}{G}): Instant
 *   "Cast this spell only during the declare attackers step and only if you've been attacked this step.
 *    Prevent all damage that would be dealt to you this turn by attacking creatures."
 *
 * These tests verify that combat damage from attacking creatures is prevented after Deep Wood resolves.
 */
class DeepWoodScenarioTest : ScenarioTestBase() {

    init {
        context("Deep Wood damage prevention") {
            test("prevents all combat damage from unblocked attackers") {
                // Setup:
                // - Player 1 attacks with creatures
                // - Player 2 casts Deep Wood during declare attackers
                // - Combat damage should be prevented
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 attacker
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3 attacker
                    .withCardInHand(2, "Deep Wood")
                    .withLandsOnBattlefield(2, "Forest", 2)     // Enough mana for {1}{G}
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val grizzlyId = game.findPermanent("Grizzly Bears")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!

                // Player 1 declares attackers
                val attackResult = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(
                            grizzlyId to game.player2Id,
                            hillGiantId to game.player2Id
                        )
                    )
                )
                withClue("Attackers should be declared successfully: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Player 1 passes priority after declaring attackers
                game.execute(PassPriority(game.player1Id))

                // Player 2 casts Deep Wood (no target needed)
                val castResult = game.castSpell(2, "Deep Wood")
                withClue("Deep Wood should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Verify Deep Wood is in graveyard (resolved)
                withClue("Deep Wood should be in graveyard after resolving") {
                    game.isInGraveyard(2, "Deep Wood") shouldBe true
                }

                // Advance to declare blockers
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Player 2 declares no blockers
                val blockResult = game.execute(DeclareBlockers(game.player2Id, emptyMap()))
                withClue("No blockers should be valid: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Advance to combat damage step
                game.advanceToPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Apply combat damage
                val combatManager = com.wingedsheep.engine.mechanics.combat.CombatManager()
                val damageResult = combatManager.applyCombatDamage(game.state)
                game.state = damageResult.state

                // Player 2's life should still be 20 (damage was prevented)
                withClue("Player 2 should still have 20 life (damage prevented by Deep Wood)") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("does not prevent combat damage without Deep Wood") {
                // Setup: Same as above but without Deep Wood
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 attacker
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3 attacker
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val grizzlyId = game.findPermanent("Grizzly Bears")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!

                // Player 1 declares attackers
                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(
                            grizzlyId to game.player2Id,
                            hillGiantId to game.player2Id
                        )
                    )
                )

                // Advance to declare blockers
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Player 2 declares no blockers
                game.execute(DeclareBlockers(game.player2Id, emptyMap()))

                // Advance to combat damage step
                game.advanceToPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Apply combat damage
                val combatManager = com.wingedsheep.engine.mechanics.combat.CombatManager()
                val damageResult = combatManager.applyCombatDamage(game.state)
                game.state = damageResult.state

                // Player 2 should have taken 5 damage (2 + 3)
                withClue("Player 2 should have 15 life (took 5 damage without Deep Wood)") {
                    game.getLifeTotal(2) shouldBe 15
                }
            }

            test("prevents damage from single attacker") {
                // Setup: Single attacker scenario
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3 attacker
                    .withCardInHand(2, "Deep Wood")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                // Player 1 declares attacker
                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(hillGiantId to game.player2Id)
                    )
                )

                // Player 1 passes priority
                game.execute(PassPriority(game.player1Id))

                // Player 2 casts Deep Wood
                game.castSpell(2, "Deep Wood")
                game.resolveStack()

                // Advance through combat
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(DeclareBlockers(game.player2Id, emptyMap()))
                game.advanceToPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Apply combat damage
                val combatManager = com.wingedsheep.engine.mechanics.combat.CombatManager()
                val damageResult = combatManager.applyCombatDamage(game.state)
                game.state = damageResult.state

                // Player 2's life should still be 20
                withClue("Player 2 should still have 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("only protects the caster, not the opponent") {
                // Setup: Player 1 has Deep Wood, Player 2 attacks
                // Deep Wood should only protect the caster (Player 1)
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2 attacker controlled by Player 2
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3 creature for Player 1 to attack back
                    .withCardInHand(1, "Deep Wood")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)  // Player 2's turn (they are attacking)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val grizzlyId = game.findPermanent("Grizzly Bears")!!

                // Player 2 declares attacker targeting Player 1
                game.execute(
                    DeclareAttackers(
                        game.player2Id,
                        mapOf(grizzlyId to game.player1Id)
                    )
                )

                // Player 2 passes priority
                game.execute(PassPriority(game.player2Id))

                // Player 1 casts Deep Wood to protect themselves
                val castResult = game.castSpell(1, "Deep Wood")
                withClue("Deep Wood should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Advance through combat
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(DeclareBlockers(game.player1Id, emptyMap()))
                game.advanceToPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Apply combat damage
                val combatManager = com.wingedsheep.engine.mechanics.combat.CombatManager()
                val damageResult = combatManager.applyCombatDamage(game.state)
                game.state = damageResult.state

                // Player 1's life should still be 20 (protected by Deep Wood)
                withClue("Player 1 should still have 20 life (protected by Deep Wood)") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
