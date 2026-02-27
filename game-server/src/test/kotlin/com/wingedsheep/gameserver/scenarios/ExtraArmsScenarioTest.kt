package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Extra Arms.
 *
 * Card reference:
 * - Extra Arms ({4}{R}): Enchantment — Aura
 *   "Enchant creature"
 *   "Whenever enchanted creature attacks, it deals 2 damage to any target."
 */
class ExtraArmsScenarioTest : ScenarioTestBase() {

    init {
        context("Extra Arms attack trigger") {
            test("enchanted creature deals 2 damage to player when attacking") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardInHand(1, "Extra Arms")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Extra Arms targeting Glory Seeker
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "Extra Arms", glorySeekerID)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Extra Arms should be on the battlefield") {
                    game.isOnBattlefield("Extra Arms") shouldBe true
                }

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Glory Seeker
                val attackResult = game.declareAttackers(mapOf("Glory Seeker" to 2))
                withClue("Declaring Glory Seeker as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // Trigger fires — select target (opponent)
                withClue("Should have pending target decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(game.player2Id))

                // Resolve the triggered ability
                game.resolveStack()

                // Advance through combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent should have taken 2 (trigger) + 2 (combat) = 4 damage
                withClue("Opponent should have taken 4 total damage") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("enchanted creature deals 2 damage to a creature when attacking") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardInHand(1, "Extra Arms")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Extra Arms targeting Glory Seeker
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Extra Arms", glorySeekerID)
                game.resolveStack()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Glory Seeker
                game.declareAttackers(mapOf("Glory Seeker" to 2))

                // Trigger fires — select Hill Giant as target
                withClue("Should have pending target decision") {
                    game.hasPendingDecision() shouldBe true
                }
                val hillGiantId = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(hillGiantId))

                // Resolve the triggered ability
                game.resolveStack()

                // Hill Giant should have taken 2 damage (3/3 with 2 damage = still alive)
                withClue("Hill Giant should still be on battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("no trigger when enchanted creature does not attack") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardInHand(1, "Extra Arms")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Extra Arms targeting Glory Seeker
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Extra Arms", glorySeekerID)
                game.resolveStack()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Hill Giant only (not the enchanted Glory Seeker)
                val attackResult = game.declareAttackers(mapOf("Hill Giant" to 2))
                withClue("Declaring Hill Giant as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // No trigger should fire - proceed to blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent should have taken only 3 combat damage from Hill Giant
                withClue("Opponent should have taken only 3 combat damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }
    }
}
