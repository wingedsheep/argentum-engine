package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Search for Azcanta // Azcanta, the Sunken Ruin (XLN #74).
 *
 *   At the beginning of your upkeep, surveil 1. Then if you have seven or more cards in your
 *   graveyard, you may transform Search for Azcanta.
 *
 * Pins that the graveyard count is read on resolution *after* the surveil (the surveilled card
 * counts toward seven), that the transform is offered even when the surveil keeps the card on
 * top (ruling), and that six cards is not enough.
 */
class SearchForAzcantaScenarioTest : ScenarioTestBase() {

    init {
        fun upkeepGame(graveyardCards: Int): TestGame {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Search for Azcanta")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
            repeat(graveyardCards) { builder.withCardInGraveyard(1, "Plains") }
            val game = builder.build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            return game
        }

        fun surveil(game: TestGame, toGraveyard: Boolean) {
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.size shouldBe 1
            if (toGraveyard) game.selectCards(listOf(decision.options.first())) else game.skipSelection()
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
        }

        test("the surveilled card counts toward seven, and accepting transforms it") {
            val game = upkeepGame(graveyardCards = 6)
            surveil(game, toGraveyard = true)

            withClue("seven cards in the graveyard after the surveil offers the transform") {
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            }
            game.answerYesNo(true)
            game.resolveStack()

            game.isOnBattlefield("Azcanta, the Sunken Ruin") shouldBe true
            game.isOnBattlefield("Search for Azcanta") shouldBe false
        }

        test("may transform even when the surveil keeps the card on top") {
            val game = upkeepGame(graveyardCards = 7)
            surveil(game, toGraveyard = false)

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.resolveStack()

            game.isOnBattlefield("Azcanta, the Sunken Ruin") shouldBe true
        }

        test("declining the transform leaves the enchantment face up") {
            val game = upkeepGame(graveyardCards = 7)
            surveil(game, toGraveyard = false)

            game.answerYesNo(false)
            game.resolveStack()

            game.isOnBattlefield("Search for Azcanta") shouldBe true
        }

        test("six cards in the graveyard after the surveil does not offer the transform") {
            val game = upkeepGame(graveyardCards = 5)
            surveil(game, toGraveyard = true)

            withClue("no transform prompt below seven cards") {
                game.hasPendingDecision() shouldBe false
            }
            game.isOnBattlefield("Search for Azcanta") shouldBe true
        }
    }
}
