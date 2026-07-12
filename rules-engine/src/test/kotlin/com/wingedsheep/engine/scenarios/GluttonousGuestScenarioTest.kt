package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Gluttonous Guest (VOW #114) — {2}{B} Creature — Vampire, 1/4.
 *
 *   When this creature enters, create a Blood token.
 *   Whenever you sacrifice a Blood token, you gain 1 life.
 *
 * Exercises the ETB Blood-token creation, and the "whenever you sacrifice a Blood token" trigger
 * by activating the Blood token's own "{1}, {T}, Discard a card, Sacrifice this artifact: Draw a
 * card" ability, which sacrifices it as part of paying its cost.
 */
class GluttonousGuestScenarioTest : ScenarioTestBase() {

    init {
        context("Gluttonous Guest") {

            test("entering the battlefield creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gluttonous Guest")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gluttonous Guest").error shouldBe null
                game.resolveStack()

                withClue("a Blood token is created on entering the battlefield") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }

            test("sacrificing a Blood token (via its own ability) gains 1 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gluttonous Guest", summoningSickness = false)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withCardInHand(1, "Grizzly Bears") // fodder to discard for Blood's cost
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val blood = game.findPermanent("Blood")!!
                val toDiscard = game.findCardsInHand(1, "Grizzly Bears").first()
                val bloodAbilityId = cardRegistry.getCard("Blood")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = blood,
                        abilityId = bloodAbilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
                    )
                )
                withClue("Activating the Blood token's ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("the Blood token was sacrificed to pay its own cost") {
                    game.findPermanents("Blood").size shouldBe 0
                }
                withClue("Gluttonous Guest's trigger gains 1 life (20 -> 21)") {
                    game.getLifeTotal(1) shouldBe 21
                }
            }
        }
    }
}
