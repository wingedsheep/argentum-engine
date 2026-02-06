package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Headhunter.
 *
 * Card reference:
 * - Headhunter ({1}{B}): Creature — Human Cleric, 1/1
 *   "Whenever Headhunter deals combat damage to a player, that player discards a card."
 *   Morph {B}
 */
class HeadhunterScenarioTest : ScenarioTestBase() {

    init {
        context("Headhunter combat damage trigger") {
            test("opponent discards a card when Headhunter deals combat damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Headhunter")
                    .withCardInHand(2, "Forest")   // Only card in hand - auto-discard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should start with 1 card in hand") {
                    game.handSize(2) shouldBe 1
                }

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Headhunter
                val attackResult = game.declareAttackers(mapOf("Headhunter" to 2))
                withClue("Declaring Headhunter as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // Advance to blockers and declare no blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires and resolves
                // With only 1 card in hand, the discard is automatic
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent should have discarded their only card
                withClue("Opponent should have 0 cards in hand after discard") {
                    game.handSize(2) shouldBe 0
                }

                withClue("Forest should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                }

                // Opponent should also have taken 1 damage
                withClue("Opponent should have taken 1 combat damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("opponent with multiple cards gets a discard decision") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Headhunter")
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should start with 2 cards in hand") {
                    game.handSize(2) shouldBe 2
                }

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Headhunter" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires, resolves,
                // and creates a discard decision
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Opponent should have a pending discard decision
                withClue("Opponent should have pending discard decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Forest to discard
                val forestInHand = game.findCardsInHand(2, "Forest")
                game.selectCards(listOf(forestInHand.first()))

                withClue("Opponent should have 1 card left in hand") {
                    game.handSize(2) shouldBe 1
                }

                withClue("Forest should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                }

                withClue("Mountain should still be in opponent's hand") {
                    game.isInHand(2, "Mountain") shouldBe true
                }
            }

            test("no discard when opponent has empty hand") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Headhunter")
                    // Opponent has no cards in hand
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should start with 0 cards in hand") {
                    game.handSize(2) shouldBe 0
                }

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Headhunter" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires but nothing to discard
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should still have 0 cards in hand") {
                    game.handSize(2) shouldBe 0
                }

                // Opponent should have taken 1 damage
                withClue("Opponent should have taken 1 combat damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("Headhunter blocked does not trigger discard") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Headhunter")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 blocker
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Headhunter" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block Headhunter with Glory Seeker
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Headhunter")))

                // Advance through combat - Headhunter dies (1/1 vs 2/2), no damage to player
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent should NOT have discarded (no combat damage to player)
                withClue("Opponent should still have 1 card in hand (no discard)") {
                    game.handSize(2) shouldBe 1
                }

                // Headhunter should be dead
                withClue("Headhunter should be in graveyard") {
                    game.isInGraveyard(1, "Headhunter") shouldBe true
                }

                // Opponent should not have taken damage
                withClue("Opponent should still be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
