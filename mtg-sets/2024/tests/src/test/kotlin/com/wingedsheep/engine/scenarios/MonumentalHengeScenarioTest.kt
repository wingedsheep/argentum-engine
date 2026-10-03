package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.MonumentalHenge
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Monumental Henge (MH3 #222): enters tapped unless you control a Plains; {T}: Add {W};
 * {2}{W}{W}, {T}: look at the top five, may reveal a historic card to hand, rest to the bottom
 * in a random order.
 */
class MonumentalHengeScenarioTest : ScenarioTestBase() {

    private val cardName = "Monumental Henge"

    private fun isTapped(game: TestGame, id: EntityId) =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("enters tapped without a Plains") {
            val game = board().withCardInHand(1, cardName).withLandsOnBattlefield(1, "Island", 1).build()
            val land = game.findCardsInHand(1, cardName).single()
            game.execute(PlayLand(game.player1Id, land)).error shouldBe null
            isTapped(game, game.findPermanent(cardName)!!) shouldBe true
        }

        test("enters untapped with a Plains") {
            val game = board().withCardInHand(1, cardName).withLandsOnBattlefield(1, "Plains", 1).build()
            val land = game.findCardsInHand(1, cardName).single()
            game.execute(PlayLand(game.player1Id, land)).error shouldBe null
            isTapped(game, game.findPermanent(cardName)!!) shouldBe false
        }

        test("activation offers only historic cards; chosen one goes to hand, rest to bottom") {
            val game = board()
                .withCardOnBattlefield(1, cardName)
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Ornithopter")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .build()
            val henge = game.findPermanent(cardName)!!
            val sixth = game.state.getLibrary(game.player1Id).last()

            game.execute(
                ActivateAbility(game.player1Id, henge, MonumentalHenge.activatedAbilities[1].id)
            ).error shouldBe null
            isTapped(game, henge) shouldBe true
            game.resolveStack()

            val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val thopter = game.findCardsInLibrary(1, "Ornithopter").single()
            pick.options shouldBe listOf(thopter)
            pick.nonSelectableOptions shouldContainAll game.findCardsInLibrary(1, "Grizzly Bears")
            game.selectCards(listOf(thopter)).error shouldBe null

            game.isInHand(1, "Ornithopter") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
            game.librarySize(1) shouldBe 5
            game.state.getLibrary(game.player1Id).first() shouldBe sixth
        }

        test("the reveal is optional") {
            val game = board()
                .withCardOnBattlefield(1, cardName)
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Ornithopter")
                .withCardInLibrary(1, "Grizzly Bears")
                .build()
            val henge = game.findPermanent(cardName)!!

            game.execute(
                ActivateAbility(game.player1Id, henge, MonumentalHenge.activatedAbilities[1].id)
            ).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection().error shouldBe null
            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 2
        }
    }
}
