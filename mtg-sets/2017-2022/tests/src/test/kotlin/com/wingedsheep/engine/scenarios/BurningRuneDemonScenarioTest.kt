package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.BurningRuneDemon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Burning-Rune Demon — {4}{B}{B} Creature — Demon 6/6 (Kaldheim #81)
 *
 * "Flying. When this creature enters, you may search your library for exactly two cards not
 *  named Burning-Rune Demon that have different names. If you do, reveal those cards. An
 *  opponent chooses one of them. Put the chosen card into your hand and the other into your
 *  graveyard, then shuffle."
 *
 * Covers the opponent's split, the "exactly two" gate (a lone find moves nothing), and
 * declining the search.
 */
class BurningRuneDemonScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BurningRuneDemon)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Casts the Demon and resolves it and its enters trigger up to the "you may" question. */
    fun GameTestDriver.castDemon() {
        val demon = putCardInHand(player1, "Burning-Rune Demon")
        giveMana(player1, Color.BLACK, 2)
        giveColorlessMana(player1, 4)
        castSpell(player1, demon).error shouldBe null
        bothPass() // Demon resolves; its enters trigger goes on the stack
        bothPass() // trigger resolves
    }

    test("an opponent chooses which of the two found cards goes to hand; the other is milled") {
        val d = driver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val lions = d.putCardOnTopOfLibrary(d.player1, "Savannah Lions")

        d.castDemon()
        d.submitYesNo(d.player1, true).error shouldBe null

        withClue("the controller searches") { d.pendingDecision?.playerId shouldBe d.player1 }
        d.submitCardSelection(d.player1, listOf(courser, lions)).error shouldBe null

        withClue("the opponent makes the choice") { d.pendingDecision?.playerId shouldBe d.player2 }
        d.submitCardSelection(d.player2, listOf(lions)).error shouldBe null

        withClue("the chosen card goes to hand") { (lions in d.getHand(d.player1)) shouldBe true }
        withClue("the other goes to the graveyard") { (courser in d.getGraveyard(d.player1)) shouldBe true }
    }

    test("finding fewer than two cards finds nothing: the card stays in the library") {
        val d = driver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")

        d.castDemon()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.submitCardSelection(d.player1, listOf(courser)).error shouldBe null

        withClue("no opponent choice for a lone card") { (d.pendingDecision?.playerId == d.player2) shouldBe false }
        withClue("the lone card is not put anywhere") {
            (courser in d.state.getLibrary(d.player1)) shouldBe true
        }
    }

    test("declining the search moves nothing") {
        val d = driver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val lions = d.putCardOnTopOfLibrary(d.player1, "Savannah Lions")

        d.castDemon()
        d.submitYesNo(d.player1, false).error shouldBe null

        withClue("both cards remain in the library") {
            (courser in d.state.getLibrary(d.player1)) shouldBe true
            (lions in d.state.getLibrary(d.player1)) shouldBe true
        }
    }
})
