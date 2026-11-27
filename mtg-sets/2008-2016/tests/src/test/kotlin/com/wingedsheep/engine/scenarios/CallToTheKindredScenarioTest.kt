package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Call to the Kindred (DKA #30) — Aura, enchant creature. "At the beginning of your upkeep, you may
 * look at the top five cards of your library. If you do, you may put a creature card that shares a
 * creature type with enchanted creature from among them onto the battlefield, then you put the rest
 * of those cards on the bottom of your library in any order."
 *
 * The selection filter reads "enchanted creature" off the Aura's host while the trigger resolves
 * over library cards: only a Bear is selectable when the Aura enchants Grizzly Bears.
 */
class CallToTheKindredScenarioTest : ScenarioTestBase() {

    private fun kindredOnBears() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(1, "Call to the Kindred", "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant") // creature, but no shared type
        .withCardInLibrary(1, "Grizzly Bears") // shares Bear
        .withCardInLibrary(1, "Shock")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Craw Wurm") // sixth card, out of reach
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("puts a creature card sharing a type with enchanted creature onto the battlefield, rest on the bottom") {
            val game = kindredOnBears()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null

            val pick = game.getPendingDecision()
            pick.shouldBeInstanceOf<SelectCardsDecision>()
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val shock = game.findCardsInLibrary(1, "Shock").single()
            val mountains = game.findCardsInLibrary(1, "Mountain")
            val wurm = game.findCardsInLibrary(1, "Craw Wurm").single()
            withClue("only the Bear is selectable among the five looked-at cards") {
                pick.options shouldContainExactlyInAnyOrder listOf(bears)
                pick.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(giant, shock) + mountains
            }

            game.selectCards(listOf(bears)).error shouldBe null
            while (game.getPendingDecision() is ReorderLibraryDecision) {
                game.keepLibraryOrder().error shouldBe null
            }
            game.resolveStack()

            withClue("the chosen Bear entered the battlefield") {
                game.findAllPermanents("Grizzly Bears").size shouldBe 2
            }
            val library = game.state.getLibrary(game.player1Id)
            library.size shouldBe 5
            withClue("the untouched sixth card is now on top") {
                library.first() shouldBe wurm
            }
            withClue("the other four looked-at cards went to the bottom") {
                library.takeLast(4) shouldContainExactlyInAnyOrder listOf(giant, shock) + mountains
            }
        }

        test("declining to look leaves the library untouched") {
            val game = kindredOnBears()
            val before = game.state.getLibrary(game.player1Id)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.state.getLibrary(game.player1Id) shouldBe before
        }
    }
}
