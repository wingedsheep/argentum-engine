package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rtr.cards.SphinxOfTheChimes
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sphinx of the Chimes (RTR #52) — {4}{U}{U} Creature — Sphinx 5/6.
 *
 *   Flying
 *   Discard two nonland cards with the same name: Draw four cards.
 */
class SphinxOfTheChimesScenarioTest : ScenarioTestBase() {

    private val abilityId = SphinxOfTheChimes.activatedAbilities.first().id

    private fun TestGame.sphinxActions(sphinx: EntityId) =
        getLegalActions(1).filter { (it.action as? ActivateAbility)?.sourceId == sphinx }

    private fun TestGame.handCards(name: String) =
        state.getHand(player1Id).filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.activate(sphinx: EntityId, discards: List<EntityId>) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = sphinx,
            abilityId = abilityId,
            costPayment = AdditionalCostPayment(discardedCards = discards),
        )
    )

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Sphinx of the Chimes")
        .apply { repeat(6) { withCardInLibrary(1, "Island") } }
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("discarding two same-named nonland cards draws four; only the pair is offered") {
            val game = base()
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Mountain")
                .withCardInHand(1, "Mountain")
                .build()
            val sphinx = game.findPermanent("Sphinx of the Chimes")!!
            val bears = game.handCards("Grizzly Bears")

            val offer = game.sphinxActions(sphinx).single()
            // The two Mountains share a name but are lands; Shock has no partner.
            offer.additionalCostInfo!!.validDiscardTargets shouldContainExactlyInAnyOrder bears

            game.activate(sphinx, bears).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            // Shock + two Mountains stayed; four Islands drawn.
            game.state.getHand(game.player1Id).size shouldBe 7
        }

        test("a hand with no same-named nonland pair can't activate, and a mixed-name payment is refused") {
            val game = base()
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .build()
            val sphinx = game.findPermanent("Sphinx of the Chimes")!!

            // Client-supplied: one Bears and the Shock don't share a name.
            val mixed = listOf(game.handCards("Grizzly Bears").first(), game.handCards("Shock").single())
            game.activate(sphinx, mixed).error shouldNotBe null
            game.state.getHand(game.player1Id).size shouldBe 3

            val lone = base()
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Island")
                .withCardInHand(1, "Island")
                .build()
            lone.sphinxActions(lone.findPermanent("Sphinx of the Chimes")!!).shouldBeEmpty()
        }
    }
}
