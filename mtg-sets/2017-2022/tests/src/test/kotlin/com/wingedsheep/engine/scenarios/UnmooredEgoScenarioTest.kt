package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.grn.cards.UnmooredEgo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unmoored Ego (GRN #212) — "Choose a card name. Search target opponent's graveyard, hand, and
 * library for up to four cards with that name and exile them. That player shuffles, then draws a
 * card for each card exiled from their hand this way."
 *
 * Pins that only the cards exiled from hand are counted for the draw.
 */
class UnmooredEgoScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + UnmooredEgo)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.cast(): ChooseOptionDecision {
        giveMana(player1, Color.BLUE, 1)
        giveMana(player1, Color.BLACK, 2)
        val spell = putCardInHand(player1, "Unmoored Ego")
        val cast = castSpellWithTargets(player1, spell, listOf(ChosenTarget.Player(player2)))
        withClue(cast.error ?: "casting Unmoored Ego failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        return pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
    }

    test("exiles up to four named cards and the opponent draws one per card exiled from their hand") {
        val d = driver()
        val inGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val inHandA = d.putCardInHand(d.player2, "Centaur Courser")
        val inHandB = d.putCardInHand(d.player2, "Centaur Courser")
        val inHandKept = d.putCardInHand(d.player2, "Centaur Courser")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val handBefore = d.getHand(d.player2).size

        val naming = d.cast()
        d.submitDecision(d.player1, OptionChosenResponse(naming.id, naming.options.indexOf("Centaur Courser")))

        val selection = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        selection.playerId shouldBe d.player1
        selection.options shouldContainExactlyInAnyOrder listOf(inGraveyard, inHandA, inHandB, inHandKept, inLibrary)
        d.submitCardSelection(d.player1, listOf(inGraveyard, inHandA, inHandB, inLibrary)).error shouldBe null

        val exile = d.getExile(d.player2)
        listOf(inGraveyard, inHandA, inHandB, inLibrary).forEach { exile shouldContain it }
        val hand = d.getHand(d.player2)
        hand shouldContain inHandKept
        hand shouldNotContain inHandA
        hand shouldNotContain inHandB
        withClue("two cards left their hand, so they draw two to replace them") {
            hand.size shouldBe handBefore
        }
    }

    test("exiling only from graveyard and library draws nothing") {
        val d = driver()
        val inGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val inHand = d.putCardInHand(d.player2, "Centaur Courser")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val handBefore = d.getHand(d.player2).size

        val naming = d.cast()
        d.submitDecision(d.player1, OptionChosenResponse(naming.id, naming.options.indexOf("Centaur Courser")))
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(inGraveyard, inLibrary)).error shouldBe null

        d.getHand(d.player2) shouldContain inHand
        d.getHand(d.player2).size shouldBe handBefore
    }
})
