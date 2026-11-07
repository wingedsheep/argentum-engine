package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.emn.cards.ThrabenStandardBearer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thraben Standard Bearer (EMN #48) — {W} Creature — Human Soldier, 1/1.
 *
 *   {1}{W}, {T}, Discard a card: Create a 1/1 white Human Soldier creature token.
 */
class ThrabenStandardBearerScenarioTest : ScenarioTestBase() {

    init {
        test("paying {1}{W}, tapping and discarding a card makes a 1/1 white Human Soldier") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Thraben Standard Bearer")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearer = game.findPermanent("Thraben Standard Bearer")!!
            val discard = game.findCardsInHand(1, "Hill Giant").single()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = bearer,
                    abilityId = ThrabenStandardBearer.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(discard)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()

            withClue("the costs were paid: the Bearer tapped and the card discarded") {
                game.state.getEntity(bearer)!!.has<TappedComponent>() shouldBe true
                game.state.getZone(game.player1Id, Zone.GRAVEYARD).contains(discard) shouldBe true
            }

            game.resolveStack()

            val token = game.findPermanent("Human Soldier Token")
            withClue("a Human Soldier token was created") { (token != null) shouldBe true }
            val projected = game.state.projectedState
            projected.getPower(token!!) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.hasColor(token, Color.WHITE) shouldBe true
            projected.hasSubtype(token, "Human") shouldBe true
            projected.hasSubtype(token, "Soldier") shouldBe true
        }
    }
}
