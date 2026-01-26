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
 * Scenario tests for Assassin's Blade's cast restrictions.
 *
 * Assassin's Blade: {1}{B} Instant
 * "Cast this spell only during the declare attackers step and only if you've been attacked this step."
 * "Destroy target nonblack attacking creature."
 *
 * These tests verify:
 * 1. Cannot be cast outside the declare attackers step
 * 2. Cannot be cast if the player hasn't been attacked this step
 * 3. CAN be cast during declare attackers when being attacked
 * 4. Successfully destroys a nonblack attacking creature
 */
class AssassinsBladeScenarioTest : ScenarioTestBase() {

    init {
        context("Assassin's Blade cast restrictions") {

            test("cannot be cast during main phase") {
                // Setup: Player 2 has Assassin's Blade in hand during player 1's main phase
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // Potential attacker
                    .withCardInHand(2, "Assassin's Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)  // Mana to cast {1}{B}
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bladeId = game.findCardsInHand(2, "Assassin's Blade").first()
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Try to cast Assassin's Blade during main phase - should FAIL
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = bladeId,
                        targets = listOf(ChosenTarget.Permanent(bearsId)),
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
                    .withCardInHand(2, "Assassin's Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare no attackers
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, emptyMap())
                )
                withClue("Declaring no attackers should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Player 1 passes priority to player 2
                game.execute(PassPriority(game.player1Id))

                val bladeId = game.findCardsInHand(2, "Assassin's Blade").first()

                // Try to cast Assassin's Blade - should FAIL (not being attacked)
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = bladeId,
                        targets = listOf(ChosenTarget.Permanent(bearsId)),
                        paymentStrategy = PaymentStrategy.AutoPay
                    )
                )

                withClue("Cast should fail - player 2 not being attacked") {
                    castResult.error shouldNotBe null
                }
            }

            test("CAN be cast during declare attackers when being attacked") {
                // Setup: Player 1 attacks player 2, player 2 can cast Assassin's Blade
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // Will attack
                    .withCardInHand(2, "Assassin's Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
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

                val bladeId = game.findCardsInHand(2, "Assassin's Blade").first()

                // Cast Assassin's Blade - should SUCCEED
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = bladeId,
                        targets = listOf(ChosenTarget.Permanent(bearsId)),
                        paymentStrategy = PaymentStrategy.AutoPay
                    )
                )

                withClue("Cast should succeed - being attacked during declare attackers: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Grizzly Bears should be destroyed
                withClue("Grizzly Bears should be destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("cannot target black attacking creature") {
                // Setup: Player 1 attacks with a black creature
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Feral Shadow")  // Black 2/1 flying creature
                    .withCardInHand(2, "Assassin's Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val shadowId = game.findPermanent("Feral Shadow")!!

                // Declare Feral Shadow as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(shadowId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Player 1 passes priority to player 2
                game.execute(PassPriority(game.player1Id))

                val bladeId = game.findCardsInHand(2, "Assassin's Blade").first()

                // Try to cast Assassin's Blade targeting the black creature - should FAIL
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = bladeId,
                        targets = listOf(ChosenTarget.Permanent(shadowId)),
                        paymentStrategy = PaymentStrategy.AutoPay
                    )
                )

                withClue("Cast should fail - cannot target black creature") {
                    castResult.error shouldNotBe null
                }
            }
        }
    }
}
