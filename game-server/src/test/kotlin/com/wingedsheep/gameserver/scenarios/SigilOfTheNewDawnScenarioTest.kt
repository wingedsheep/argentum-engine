package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sigil of the New Dawn.
 *
 * Card reference:
 * - Sigil of the New Dawn ({3}{W}): Enchantment
 *   "Whenever a creature is put into your graveyard from the battlefield,
 *    you may pay {1}{W}. If you do, return that card to your hand."
 */
class SigilOfTheNewDawnScenarioTest : ScenarioTestBase() {

    init {
        context("Sigil of the New Dawn") {
            test("returns creature to hand when player pays {1}{W}") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Sigil of the New Dawn")
                    .withCardOnBattlefield(1, "Nantuko Husk")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Plains", 2) // For {1}{W} payment
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val huskId = game.findPermanent("Nantuko Husk")!!
                val glorySeekerId = game.findPermanent("Glory Seeker")!!

                val cardDef = cardRegistry.getCard("Nantuko Husk")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Nantuko Husk, sacrificing Glory Seeker as cost
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = huskId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(glorySeekerId)
                        )
                    )
                )
                withClue("Ability should activate successfully") {
                    result.error shouldBe null
                }

                // Sigil trigger and Husk ability are on the stack.
                // Resolve stack - Sigil trigger resolves first (LIFO), creating YesNo decision.
                game.resolveStack()

                // Sigil asks "Pay {1}{W}?"
                withClue("Sigil should ask to pay {1}{W}") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Pay mana if mana source selection is needed
                if (game.hasPendingDecision()) {
                    game.submitManaSourcesAutoPay()
                }

                // Resolve remaining stack (Husk's +2/+2 ability)
                game.resolveStack()

                // Glory Seeker should be back in player's hand
                withClue("Glory Seeker should be in hand after Sigil returns it") {
                    game.isInHand(1, "Glory Seeker") shouldBe true
                }
                withClue("Glory Seeker should not be in graveyard") {
                    game.isInGraveyard(1, "Glory Seeker") shouldBe false
                }
            }

            test("creature stays in graveyard when player declines to pay") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Sigil of the New Dawn")
                    .withCardOnBattlefield(1, "Nantuko Husk")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val huskId = game.findPermanent("Nantuko Husk")!!
                val glorySeekerId = game.findPermanent("Glory Seeker")!!

                val cardDef = cardRegistry.getCard("Nantuko Husk")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Nantuko Husk, sacrificing Glory Seeker
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = huskId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(glorySeekerId)
                        )
                    )
                )

                // Resolve stack - Sigil trigger resolves, creating YesNo decision
                game.resolveStack()

                // Decline to pay {1}{W}
                game.answerYesNo(false)

                // Resolve remaining stack (Husk's ability)
                game.resolveStack()

                // Glory Seeker should stay in graveyard
                withClue("Glory Seeker should remain in graveyard") {
                    game.isInGraveyard(1, "Glory Seeker") shouldBe true
                }
                withClue("Glory Seeker should not be in hand") {
                    game.isInHand(1, "Glory Seeker") shouldBe false
                }
            }

            test("does not trigger for opponent's creatures dying") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Sigil of the New Dawn")
                    .withCardOnBattlefield(2, "Nantuko Husk")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2) // Opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val huskId = game.findPermanent("Nantuko Husk")!!
                val glorySeekerId = game.findPermanent("Glory Seeker")!!

                val cardDef = cardRegistry.getCard("Nantuko Husk")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Opponent sacrifices their own Glory Seeker to Nantuko Husk
                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = huskId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(glorySeekerId)
                        )
                    )
                )

                // No Sigil trigger - should not have a pending decision
                withClue("Sigil should not trigger for opponent's creature dying") {
                    game.hasPendingDecision() shouldBe false
                }

                // Glory Seeker should be in opponent's graveyard
                withClue("Glory Seeker should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Glory Seeker") shouldBe true
                }
            }
        }
    }
}
