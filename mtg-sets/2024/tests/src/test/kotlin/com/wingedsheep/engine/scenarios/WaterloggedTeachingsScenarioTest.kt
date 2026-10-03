package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Waterlogged Teachings // Inundated Archive (MH3).
 *
 * Front: "Search your library for an instant card or a card with flash, reveal it, put it into
 * your hand, then shuffle." Back: "This land enters tapped. {T}: Add {U} or {B}."
 *
 * The search must offer instants and flash cards (here a flash creature) but not sorceries or
 * plain creatures — the union filter is the whole card.
 */
class WaterloggedTeachingsScenarioTest : ScenarioTestBase() {

    init {
        test("search offers instants and flash cards only, and puts the pick into hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Waterlogged Teachings")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Faerie Harbinger")
                .withCardInLibrary(1, "Divination")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Waterlogged Teachings").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            val offered = decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
            withClue("only the instant and the flash card are findable") {
                offered shouldContainExactlyInAnyOrder listOf("Lightning Bolt", "Faerie Harbinger")
            }

            game.selectCards(listOf(game.findCardsInLibrary(1, "Faerie Harbinger").single())).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Faerie Harbinger") shouldBe true
            game.isInGraveyard(1, "Waterlogged Teachings") shouldBe true
        }

        test("Inundated Archive enters tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Waterlogged Teachings")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.state.getHand(game.player1Id).single()
            game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null

            val land = game.findPermanent("Inundated Archive")!!
            game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
        }
    }
}
