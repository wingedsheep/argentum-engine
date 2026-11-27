package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Descendants' Path (AVR #173) — {2}{G} Enchantment.
 *
 *   At the beginning of your upkeep, reveal the top card of your library. If it's a creature card
 *   that shares a creature type with a creature you control, you may cast it without paying its
 *   mana cost. If you don't cast it, put it on the bottom of your library.
 *
 * Covers the free cast of a type-sharing creature, a creature sharing no type (it can't be cast and
 * goes to the bottom), declining the cast (bottom), and a Changeling card in the library sharing a
 * type with any creature (CR 702.73a) — the predicate `sharingCreatureTypeWithPermanentYouControl`.
 */
class DescendantsPathScenarioTest : ScenarioTestBase() {

    /** Descendants' Path and a Grizzly Bears (Bear) for player 1; [top] on top of their library. */
    private fun upkeepWith(top: String): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Descendants' Path")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInLibrary(1, top)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    private fun TestGame.isOnBottomOfLibrary(name: String): Boolean {
        val id = findCardsInLibrary(1, name).singleOrNull() ?: return false
        return state.getLibrary(player1Id).last() == id
    }

    init {
        context("Descendants' Path") {

            test("a revealed creature sharing a creature type is cast without paying its mana cost") {
                val game = upkeepWith("Grizzly Bears")
                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val revealed = game.findCardsInLibrary(1, "Grizzly Bears").single()
                choice.options shouldBe listOf(revealed)
                game.selectCards(listOf(revealed)).error shouldBe null
                game.resolveStack()

                withClue("the second Grizzly Bears was cast from the library and resolved") {
                    game.findPermanents("Grizzly Bears").size shouldBe 2
                    game.findCardsInLibrary(1, "Grizzly Bears") shouldBe emptyList()
                    game.librarySize(1) shouldBe 2
                }
            }

            test("a revealed creature sharing no creature type can't be cast and goes to the bottom") {
                val game = upkeepWith("Hill Giant")
                if (game.getPendingDecision() is SelectCardsDecision) {
                    val giant = game.findCardsInLibrary(1, "Hill Giant").single()
                    withClue("a Giant shares no creature type with a Bear") {
                        game.selectCards(listOf(giant)).error shouldNotBe null
                    }
                    game.selectCards(emptyList()).error shouldBe null
                }
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBottomOfLibrary("Hill Giant") shouldBe true
                game.librarySize(1) shouldBe 3
            }

            test("declining the free cast puts the revealed card on the bottom") {
                val game = upkeepWith("Grizzly Bears")
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.findPermanents("Grizzly Bears").size shouldBe 1
                game.isOnBottomOfLibrary("Grizzly Bears") shouldBe true
            }

            test("a Changeling card in the library shares a creature type with any creature") {
                val game = upkeepWith("Fire-Belly Changeling")
                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val changeling = game.findCardsInLibrary(1, "Fire-Belly Changeling").single()
                choice.options shouldBe listOf(changeling)
                game.selectCards(listOf(changeling)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Fire-Belly Changeling") shouldBe true
            }

            test("a revealed noncreature card goes to the bottom") {
                val game = upkeepWith("Island")
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(emptyList()).error shouldBe null
                }
                game.resolveStack()

                game.isOnBottomOfLibrary("Island") shouldBe true
            }
        }
    }
}
