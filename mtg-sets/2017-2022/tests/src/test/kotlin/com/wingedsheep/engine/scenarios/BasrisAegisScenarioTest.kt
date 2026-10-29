package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Basri's Aegis (M21 #322) — {2}{W}{W} Sorcery.
 *
 * Put a +1/+1 counter on each of up to two target creatures. You may search your library and/or
 * graveyard for a card named Basri, Devoted Paladin, reveal it, and put it into your hand. If you
 * search your library this way, shuffle.
 *
 * The zone choice itself is pinned by `SearchLibraryAndOrGraveyardTest`; this pins the card's
 * counters, its named-card filter, and that "you may" can decline.
 */
class BasrisAegisScenarioTest : ScenarioTestBase() {

    private fun cast(game: TestGame, vararg creatures: String) {
        val aegis = game.findCardsInHand(1, "Basri's Aegis").single()
        val targets = creatures.map { ChosenTarget.Permanent(game.findPermanent(it)!!) }
        game.execute(CastSpell(game.player1Id, aegis, targets)).error shouldBe null
        game.resolveStack()
    }

    private fun choose(game: TestGame, option: String) {
        val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        game.submitDecision(OptionChosenResponse(choice.id, choice.options.indexOf(option))).error shouldBe null
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Basri's Aegis")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Basri, Devoted Paladin")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Island")
        .withCardInGraveyard(1, "Basri, Devoted Paladin")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("counters on two creatures, then Basri, Devoted Paladin from the library to hand") {
            val game = board()
            cast(game, "Grizzly Bears", "Hill Giant")
            choose(game, "Search your library")
            val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val basri = game.findCardsInLibrary(1, "Basri, Devoted Paladin").single()
            search.options shouldBe listOf(basri)
            game.selectCards(listOf(basri)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 3
            game.state.projectedState.getPower(game.findPermanent("Hill Giant")!!) shouldBe 4
            game.findCardsInHand(1, "Basri, Devoted Paladin").single() shouldBe basri
            game.isInGraveyard(1, "Basri, Devoted Paladin") shouldBe true
        }

        test("graveyard search returns the copy in the graveyard; zero targets is legal") {
            val game = board()
            cast(game)
            choose(game, "Search your graveyard")
            game.resolveStack()
            game.isInHand(1, "Basri, Devoted Paladin") shouldBe true
            game.isInGraveyard(1, "Basri, Devoted Paladin") shouldBe false
            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }

        test("declining the search leaves both copies where they are") {
            val game = board()
            cast(game, "Grizzly Bears")
            choose(game, "Don't search")
            game.resolveStack()
            game.isInHand(1, "Basri, Devoted Paladin") shouldBe false
            game.findCardsInLibrary(1, "Basri, Devoted Paladin").size shouldBe 1
            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 3
        }
    }
}
