package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.TwistedLandscape
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Twisted Landscape (MH3): {T}: Add {C}; {T}, sacrifice: fetch a basic Swamp/Mountain/Forest
 * tapped; cycling {B}{R}{G}.
 */
class TwistedLandscapeScenarioTest : ScenarioTestBase() {

    private val cardName = "Twisted Landscape"
    private val card = TwistedLandscape
    private val eligibleBasics = listOf("Swamp", "Mountain", "Forest")
    private val ineligibleBasic = "Island"
    private val ineligibleNonbasic = "Blood Crypt"
    private val cyclingSources = listOf("Swamp", "Mountain", "Forest")

    private fun isTapped(game: TestGame, id: com.wingedsheep.sdk.model.EntityId) =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, cardName)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("taps for {C}") {
            val game = board().withCardInLibrary(1, ineligibleBasic).build()
            val land = game.findPermanent(cardName)!!

            val result = game.execute(
                ActivateAbility(game.player1Id, land, card.activatedAbilities[0].id)
            )
            result.error shouldBe null

            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
            isTapped(game, land) shouldBe true
        }

        test("sacrifice fetches an eligible basic tapped; other lands are not offered") {
            val builder = board()
            (eligibleBasics + ineligibleBasic + ineligibleNonbasic).forEach { builder.withCardInLibrary(1, it) }
            val game = builder.build()
            val land = game.findPermanent(cardName)!!

            game.execute(
                ActivateAbility(game.player1Id, land, card.activatedAbilities[1].id)
            ).error shouldBe null
            game.isInGraveyard(1, cardName) shouldBe true
            game.resolveStack()

            val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val eligibleIds = eligibleBasics.flatMap { game.findCardsInLibrary(1, it) }
            pick.options shouldContainExactlyInAnyOrder eligibleIds

            val chosenName = eligibleBasics.last()
            game.selectCards(game.findCardsInLibrary(1, chosenName)).error shouldBe null

            val fetched = game.findPermanent(chosenName)!!
            isTapped(game, fetched) shouldBe true
            game.librarySize(1) shouldBe eligibleBasics.size + 1
        }

        test("cycling draws a card") {
            val builder = board().withCardInHand(1, cardName).withCardInLibrary(1, ineligibleBasic)
            cyclingSources.forEach { builder.withLandsOnBattlefield(1, it, 1) }
            val game = builder.build()

            game.cycleCard(1, cardName).error shouldBe null

            game.isInGraveyard(1, cardName) shouldBe true
            game.isInHand(1, ineligibleBasic) shouldBe true
            cyclingSources.forEach { isTapped(game, game.findPermanent(it)!!) shouldBe true }
        }
    }
}
