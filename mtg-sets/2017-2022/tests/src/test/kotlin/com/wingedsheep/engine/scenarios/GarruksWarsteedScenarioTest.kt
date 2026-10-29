package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Garruk's Warsteed (M21 #337) — {3}{G}{G} Creature — Rhino 3/5, vigilance.
 *
 * When this creature enters, you may search your library and/or graveyard for a card named Garruk,
 * Savage Herald, reveal it, and put it into your hand. If you search your library this way, shuffle.
 *
 * Pins the enters trigger wiring: the zone choice (with "Don't search") is asked as the trigger
 * resolves, and a combined search offers the named card from both zones.
 */
class GarruksWarsteedScenarioTest : ScenarioTestBase() {

    private fun castWarsteed(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Garruk's Warsteed")
            .withLandsOnBattlefield(1, "Forest", 5)
            .withCardInLibrary(1, "Garruk, Savage Herald")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Island")
            .withCardInGraveyard(1, "Garruk, Savage Herald")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Garruk's Warsteed").error shouldBe null
        game.resolveStack()
        return game
    }

    private fun choose(game: TestGame, option: String) {
        val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        game.submitDecision(OptionChosenResponse(choice.id, choice.options.indexOf(option))).error shouldBe null
    }

    init {
        test("enters: search both zones and take Garruk, Savage Herald from the graveyard") {
            val game = castWarsteed()
            game.findPermanent("Garruk's Warsteed") shouldNotBe null
            choose(game, "Search your library and graveyard")
            val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val fromGraveyard = game.state.getGraveyard(game.player1Id).single()
            val fromLibrary = game.findCardsInLibrary(1, "Garruk, Savage Herald").single()
            search.options shouldContainExactlyInAnyOrder listOf(fromLibrary, fromGraveyard)
            game.selectCards(listOf(fromGraveyard)).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Garruk, Savage Herald").single() shouldBe fromGraveyard
            game.findCardsInLibrary(1, "Garruk, Savage Herald").single() shouldBe fromLibrary
        }

        test("enters: declining leaves Garruk, Savage Herald in place") {
            val game = castWarsteed()
            choose(game, "Don't search")
            game.resolveStack()
            game.isInHand(1, "Garruk, Savage Herald") shouldBe false
            game.isInGraveyard(1, "Garruk, Savage Herald") shouldBe true
        }
    }
}
