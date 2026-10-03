package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.UrzasCave
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Urza's Cave (MH3): {T}: Add {C}; {3}, {T}, sacrifice: search for any land card, put it onto the
 * battlefield tapped, then shuffle.
 */
class UrzasCaveScenarioTest : ScenarioTestBase() {

    private val cardName = "Urza's Cave"

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
            val game = board().withCardInLibrary(1, "Plains").build()
            val cave = game.findPermanent(cardName)!!

            game.execute(
                ActivateAbility(game.player1Id, cave, UrzasCave.activatedAbilities[0].id)
            ).error shouldBe null

            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
            isTapped(game, cave) shouldBe true
        }

        test("sacrifice fetches any land card (basic or nonbasic) tapped; nonlands are not offered") {
            val game = board()
                .withLandsOnBattlefield(1, "Wastes", 3)
                .withCardInLibrary(1, "Urza's Tower")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .build()
            val cave = game.findPermanent(cardName)!!

            game.execute(
                ActivateAbility(game.player1Id, cave, UrzasCave.activatedAbilities[1].id)
            ).error shouldBe null
            game.isInGraveyard(1, cardName) shouldBe true
            game.resolveStack()

            val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            pick.options shouldContainExactlyInAnyOrder
                game.findCardsInLibrary(1, "Urza's Tower") + game.findCardsInLibrary(1, "Forest")

            game.selectCards(game.findCardsInLibrary(1, "Urza's Tower")).error shouldBe null

            val fetched = game.findPermanent("Urza's Tower")
            fetched shouldNotBe null
            isTapped(game, fetched!!) shouldBe true
            game.librarySize(1) shouldBe 2
        }

        test("sacrifice ability needs {3}") {
            val game = board().withCardInLibrary(1, "Forest").build()
            val cave = game.findPermanent(cardName)!!

            game.execute(
                ActivateAbility(game.player1Id, cave, UrzasCave.activatedAbilities[1].id)
            ).error shouldNotBe null
            game.findPermanent(cardName) shouldNotBe null
        }
    }
}
