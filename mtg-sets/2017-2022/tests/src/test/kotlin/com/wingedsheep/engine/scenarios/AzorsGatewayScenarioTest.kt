package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.AzorsGateway
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Azor's Gateway // Sanctum of the Sun (RIX #176).
 *
 * Pins the linked-exile distinct-mana-value count read on resolution: the fifth different mana
 * value gains 5 life, untaps and transforms the Gateway; a repeated mana value doesn't count,
 * and neither does a card exiled by something else.
 */
class AzorsGatewayScenarioTest : ScenarioTestBase() {

    private val gatewayAbilityId get() = AzorsGateway.activatedAbilities[0].id

    /**
     * Gateway on the battlefield with one Island to pay {1}; [inExile] seeded in player 1's exile,
     * [inHand] in hand, and a Plains on top of the library to draw.
     */
    private fun game(inExile: List<String>, inHand: String): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Azor's Gateway")
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardInHand(1, inHand)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
        inExile.forEach { builder = builder.withCardInExile(1, it) }
        return builder
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    private fun TestGame.name(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    private fun TestGame.exiled(cardName: String): EntityId =
        state.getExile(player1Id).first { name(it) == cardName }

    private fun TestGame.link(gateway: EntityId, cards: List<EntityId>) {
        state = state.updateEntity(gateway) { it.with(LinkedExileComponent(cards)) }
    }

    /** Activate the Gateway, then exile [toExile] from hand when the resolution asks. */
    private fun TestGame.activateAndExile(gateway: EntityId, toExile: String) {
        val result = execute(ActivateAbility(player1Id, gateway, gatewayAbilityId))
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
        withClue("the resolution asks which hand card to exile") {
            (getPendingDecision() is SelectCardsDecision) shouldBe true
        }
        // Looked up after the draw, so the drawn card can be the one exiled.
        val handCard = findCardsInHand(1, toExile).first()
        selectCards(listOf(handCard)).error shouldBe null
        resolveStack()
    }

    /** Mana values 0 (Forest), 1 (Giant Growth), 2 (Grizzly Bears), 3 (Wind Drake). */
    private val fourValues = listOf("Forest", "Giant Growth", "Grizzly Bears", "Wind Drake")

    init {
        test("the fifth different mana value gains 5 life, untaps and transforms it") {
            val game = game(inExile = fourValues, inHand = "Hill Giant")
            val gateway = game.findPermanent("Azor's Gateway")!!
            game.link(gateway, fourValues.map { game.exiled(it) })

            game.activateAndExile(gateway, "Hill Giant")

            val giant = game.exiled("Hill Giant")
            withClue("the Hill Giant joined the linked pile, face up") {
                game.state.getEntity(gateway)!!.get<LinkedExileComponent>()!!.exiledIds shouldContain giant
                game.state.getEntity(giant)!!.has<FaceDownComponent>() shouldBe false
            }
            game.getLifeTotal(1) shouldBe 25
            game.name(gateway) shouldBe "Sanctum of the Sun"
            withClue("the Gateway was untapped as part of the payoff") {
                game.state.getEntity(gateway)!!.has<TappedComponent>() shouldBe false
            }
        }

        test("a repeated mana value doesn't count toward the five") {
            val game = game(inExile = fourValues, inHand = "Hill Giant")
            val gateway = game.findPermanent("Azor's Gateway")!!
            game.link(gateway, fourValues.map { game.exiled(it) })

            // The drawn Plains (mana value 0) repeats the Forest's value.
            game.activateAndExile(gateway, "Plains")

            game.name(gateway) shouldBe "Azor's Gateway"
            game.getLifeTotal(1) shouldBe 20
            withClue("no payoff, so the Gateway stays tapped") {
                game.state.getEntity(gateway)!!.has<TappedComponent>() shouldBe true
            }
        }

        test("cards exiled by something else don't count") {
            val game = game(inExile = fourValues, inHand = "Hill Giant")
            val gateway = game.findPermanent("Azor's Gateway")!!
            // Only three of the four exiled cards are linked; Wind Drake was exiled by something else.
            game.link(gateway, listOf("Forest", "Giant Growth", "Grizzly Bears").map { game.exiled(it) })

            game.activateAndExile(gateway, "Hill Giant")

            withClue("0, 1, 2 and 4 are only four different mana values") {
                game.name(gateway) shouldBe "Azor's Gateway"
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
