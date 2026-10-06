package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Glint-Horn Buccaneer (M20 #141).
 *
 * "Whenever you discard a card, this creature deals 1 damage to each opponent.
 *  {1}{R}, Discard a card: Draw a card. Activate only if this creature is attacking."
 *
 * Covers the "activate only if attacking" restriction and the discard cost feeding the
 * discard trigger.
 */
class GlintHornBuccaneerScenarioTest : ScenarioTestBase() {

    private val abilityId =
        cardRegistry.getCard("Glint-Horn Buccaneer")!!.activatedAbilities.first().id

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Glint-Horn Buccaneer", summoningSickness = false)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInHand(1, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Glint-Horn Buccaneer") {

            test("the draw ability can't be activated while it isn't attacking") {
                val game = game()
                val buccaneer = game.findPermanent("Glint-Horn Buccaneer")!!
                val bears = game.findCardsInHand(1, "Grizzly Bears").first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = buccaneer,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(bears)),
                    )
                )
                withClue("activation outside combat is rejected") { (result.error != null) shouldBe true }
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("while attacking, discarding to the ability draws a card and pings each opponent") {
                val game = game()
                val buccaneer = game.findPermanent("Glint-Horn Buccaneer")!!
                val bears = game.findCardsInHand(1, "Grizzly Bears").first()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glint-Horn Buccaneer" to 2)).error shouldBe null

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = buccaneer,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(bears)),
                    )
                )
                withClue("activation while attacking should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("the discarded card is in the graveyard") {
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("drew a card") { game.findCardsInHand(1, "Plains").size shouldBe 1 }
                withClue("discard trigger dealt 1 damage to the opponent") { game.getLifeTotal(2) shouldBe 19 }
            }
        }
    }
}
