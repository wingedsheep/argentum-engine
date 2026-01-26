package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Blessed Reversal's cast restrictions.
 *
 * Blessed Reversal: {1}{W} Instant
 * "Cast this spell only during the declare attackers step and only if you've been attacked this step."
 * "You gain 3 life for each creature attacking you."
 *
 * These tests verify:
 * 1. Cannot be cast outside the declare attackers step
 * 2. Cannot be cast if the player hasn't been attacked this step
 * 3. CAN be cast during declare attackers when being attacked
 * 4. Successfully gains life based on attacking creatures
 */
class BlessedReversalScenarioTest : ScenarioTestBase() {

    init {
        context("Blessed Reversal cast restrictions") {

            test("cannot be cast during main phase") {
                // Setup: Player 2 has Blessed Reversal in hand during player 1's main phase
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // Potential attacker
                    .withCardInHand(2, "Blessed Reversal")
                    .withLandsOnBattlefield(2, "Plains", 2)  // Mana to cast {1}{W}
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val reversalId = game.findCardsInHand(2, "Blessed Reversal").first()

                // Try to cast Blessed Reversal during main phase - should FAIL
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = reversalId,
                        targets = emptyList(),
                        paymentStrategy = PaymentStrategy.AutoPay
                    )
                )

                withClue("Cast should fail - not in declare attackers step") {
                    castResult.error shouldNotBe null
                }
            }

            test("cannot be cast during declare attackers if not being attacked") {
                // Setup: Player 1 has an attacker but doesn't attack player 2
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // Will not attack
                    .withCardInHand(2, "Blessed Reversal")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Declare no attackers
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, emptyMap())
                )
                withClue("Declaring no attackers should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Player 1 passes priority to player 2
                game.execute(PassPriority(game.player1Id))

                val reversalId = game.findCardsInHand(2, "Blessed Reversal").first()

                // Try to cast Blessed Reversal - should FAIL (not being attacked)
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = reversalId,
                        targets = emptyList(),
                        paymentStrategy = PaymentStrategy.AutoPay
                    )
                )

                withClue("Cast should fail - player 2 not being attacked") {
                    castResult.error shouldNotBe null
                }
            }

            test("CAN be cast during declare attackers when being attacked") {
                // Setup: Player 1 attacks player 2, player 2 can cast Blessed Reversal
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // Will attack
                    .withCardInHand(2, "Blessed Reversal")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withLifeTotal(2, 10)  // Start at 10 life to see life gain
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare Grizzly Bears as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(bearsId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Player 1 passes priority to player 2
                game.execute(PassPriority(game.player1Id))

                val reversalId = game.findCardsInHand(2, "Blessed Reversal").first()

                // Cast Blessed Reversal - should SUCCEED
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = reversalId,
                        targets = emptyList(),
                        paymentStrategy = PaymentStrategy.AutoPay
                    )
                )

                withClue("Cast should succeed - being attacked during declare attackers: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Player should gain 3 life (1 creature attacking)
                withClue("Player 2 should gain 3 life (10 + 3 = 13)") {
                    game.getLifeTotal(2) shouldBe 13
                }
            }

            test("gains life based on number of attackers") {
                // Setup: Player 1 attacks with multiple creatures
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Elite Cat Warrior")
                    .withCardOnBattlefield(1, "Feral Shadow")
                    .withCardInHand(2, "Blessed Reversal")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withLifeTotal(2, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val catId = game.findPermanent("Elite Cat Warrior")!!
                val shadowId = game.findPermanent("Feral Shadow")!!

                // Declare all three creatures as attackers
                val attackResult = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(
                            bearsId to game.player2Id,
                            catId to game.player2Id,
                            shadowId to game.player2Id
                        )
                    )
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Player 1 passes priority to player 2
                game.execute(PassPriority(game.player1Id))

                val reversalId = game.findCardsInHand(2, "Blessed Reversal").first()

                // Cast Blessed Reversal
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = reversalId,
                        targets = emptyList(),
                        paymentStrategy = PaymentStrategy.AutoPay
                    )
                )

                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Player should gain 9 life (3 creatures × 3 life each)
                withClue("Player 2 should gain 9 life (10 + 9 = 19)") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }
    }
}
