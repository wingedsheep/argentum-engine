package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Guilty Conscience.
 *
 * Card reference:
 * - Guilty Conscience ({W}): Enchantment — Aura
 *   "Enchant creature"
 *   "Whenever enchanted creature deals damage, Guilty Conscience deals that much damage to that creature."
 */
class GuiltyConscienceScenarioTest : ScenarioTestBase() {

    init {
        context("Guilty Conscience damage reflection") {
            test("enchanted creature takes damage equal to combat damage dealt") {
                val game = scenario()
                    .withPlayers("Enchanter", "Defender")
                    .withCardInHand(1, "Guilty Conscience")
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3 creature
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Guilty Conscience targeting Hill Giant
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val castResult = game.castSpell(1, "Guilty Conscience", hillGiantId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Guilty Conscience should be on the battlefield") {
                    game.isOnBattlefield("Guilty Conscience") shouldBe true
                }

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Hill Giant
                val attackResult = game.declareAttackers(mapOf("Hill Giant" to 2))
                withClue("Declaring Hill Giant as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // No blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent took 3 combat damage
                withClue("Opponent should have taken 3 combat damage") {
                    game.getLifeTotal(2) shouldBe 17
                }

                // Hill Giant took 3 damage from Guilty Conscience (3/3 with 3 damage = dead)
                withClue("Hill Giant should be destroyed by Guilty Conscience damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }

            test("enchanted creature on opponent's side reflects combat damage") {
                val game = scenario()
                    .withPlayers("Enchanter", "Attacker")
                    .withCardInHand(1, "Guilty Conscience")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 opponent creature
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Guilty Conscience on opponent's creature
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "Guilty Conscience", glorySeekerID)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Pass to opponent's turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Opponent attacks with Glory Seeker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attackResult = game.declareAttackers(mapOf("Glory Seeker" to 1))
                withClue("Attack should succeed") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Player 1 took 2 combat damage
                withClue("Player 1 should have taken 2 combat damage") {
                    game.getLifeTotal(1) shouldBe 18
                }

                // Glory Seeker took 2 damage from Guilty Conscience (2/2 with 2 damage = dead)
                withClue("Glory Seeker should be destroyed by Guilty Conscience damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }

            test("creature with toughness greater than power survives") {
                val game = scenario()
                    .withPlayers("Enchanter", "Defender")
                    .withCardInHand(1, "Guilty Conscience")
                    .withCardOnBattlefield(1, "Border Guard") // 1/4 creature
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Guilty Conscience targeting Border Guard (1/4)
                val borderGuardId = game.findPermanent("Border Guard")!!
                val castResult = game.castSpell(1, "Guilty Conscience", borderGuardId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Attack
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Border Guard" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent took 1 damage
                withClue("Opponent should have taken 1 combat damage") {
                    game.getLifeTotal(2) shouldBe 19
                }

                // Border Guard took 1 damage from Guilty Conscience (1/4 with 1 damage = survives)
                withClue("Border Guard should survive") {
                    game.isOnBattlefield("Border Guard") shouldBe true
                }
            }
        }
    }
}
