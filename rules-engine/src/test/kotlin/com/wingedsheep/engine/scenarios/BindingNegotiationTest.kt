package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BindingNegotiation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Binding Negotiation (OTJ #78).
 *
 * "Target opponent reveals their hand. You may choose a nonland card from it. If you do,
 *  they discard it. Otherwise, you may put a face-up exiled card they own into their graveyard."
 *
 * Two paths:
 *  - You choose a nonland card → it's discarded; the "Otherwise" half does NOT run.
 *  - You decline the discard → you may bin a face-up exiled card the opponent owns.
 */
class BindingNegotiationTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BindingNegotiation))
        return driver
    }

    fun castBindingNegotiation(driver: GameTestDriver, you: com.wingedsheep.sdk.model.EntityId, opponent: com.wingedsheep.sdk.model.EntityId) {
        val card = driver.putCardInHand(you, "Binding Negotiation")
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)
        // Target the opponent at cast time (the helper builds a ChosenTarget.Player).
        driver.castSpell(you, card, targets = listOf(opponent)).isSuccess shouldBe true
        driver.bothPass() // resolve the spell onto the stack -> begin its resolution
    }

    test("choosing a nonland card discards it; the otherwise clause does not run") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a nonland card in hand and a face-up card in exile.
        val nonland = driver.putCardInHand(opponent, "Grizzly Bears")
        val exiled = driver.putCardInExile(opponent, "Hill Giant")
        val gyBefore = driver.getGraveyard(opponent).size

        castBindingNegotiation(driver, you, opponent)

        // Drain decisions: choose the nonland card to discard.
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision is SelectCardsDecision) && guard++ < 30) {
            if (driver.state.pendingDecision is SelectCardsDecision) {
                driver.submitCardSelection(you, listOf(nonland))
            } else {
                driver.bothPass()
            }
        }

        // The nonland card was discarded to the opponent's graveyard.
        driver.getGraveyard(opponent).contains(nonland) shouldBe true
        // The exiled card was NOT binned — the otherwise clause is skipped when a discard happened.
        driver.getExile(opponent).contains(exiled) shouldBe true
        (driver.getGraveyard(opponent).size - gyBefore) shouldBe 1
    }

    test("declining the discard lets you bin a face-up exiled card they own") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a nonland card in hand (we'll decline it) and a face-up exiled card.
        val nonland = driver.putCardInHand(opponent, "Grizzly Bears")
        val exiled = driver.putCardInExile(opponent, "Hill Giant")

        castBindingNegotiation(driver, you, opponent)

        // Drain decisions: decline the discard (empty selection), then bin the exiled card.
        var guard = 0
        var declinedDiscard = false
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision is SelectCardsDecision) && guard++ < 30) {
            if (driver.state.pendingDecision is SelectCardsDecision) {
                if (!declinedDiscard) {
                    driver.submitCardSelection(you, emptyList()) // decline the nonland discard
                    declinedDiscard = true
                } else {
                    driver.submitCardSelection(you, listOf(exiled)) // bin the exiled card
                }
            } else {
                driver.bothPass()
            }
        }

        // Nothing was discarded from hand.
        driver.getHand(opponent).contains(nonland) shouldBe true
        // The face-up exiled card moved to the opponent's graveyard.
        driver.getExile(opponent).contains(exiled) shouldBe false
        driver.getGraveyard(opponent).contains(exiled) shouldBe true
    }
})
