package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sorcerer's Broom (ELD #232) — {2} Artifact Creature — Spirit 2/1.
 *
 * "Whenever you sacrifice another permanent, you may pay {3}. If you do, create a token that's a
 * copy of this creature."
 *
 * Sacrifices a Blood token ({1}, {T}, Discard a card, Sacrifice this token: Draw a card) to fire
 * the trigger.
 */
class SorcerersBroomScenarioTest : ScenarioTestBase() {

    private fun sacrificeBlood(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Sorcerer's Broom")
            .withCardOnBattlefield(1, "Blood", isToken = true)
            .withCardInHand(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val blood = game.findPermanent("Blood")!!
        val toDiscard = game.findCardsInHand(1, "Swamp").first()
        val bloodAbilityId = cardRegistry.getCard("Blood")!!.activatedAbilities.first().id

        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = blood,
                abilityId = bloodAbilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
            )
        ).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Sorcerer's Broom") {

            test("paying {3} after sacrificing another permanent creates a token copy") {
                val game = sacrificeBlood()

                withClue("the sacrifice offers the {3} payment") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                val brooms = game.findPermanents("Sorcerer's Broom")
                withClue("the original Broom plus one token copy") {
                    brooms.size shouldBe 2
                }
                val token = brooms.single { game.state.getEntity(it)!!.has<TokenComponent>() }
                withClue("the token copies the Broom's 2/1 body") {
                    game.state.projectedState.getPower(token) shouldBe 2
                    game.state.projectedState.getToughness(token) shouldBe 1
                }
            }

            test("declining to pay creates no token") {
                val game = sacrificeBlood()

                game.answerYesNo(false)
                game.resolveStack()

                game.findPermanents("Sorcerer's Broom").size shouldBe 1
            }
        }
    }
}
