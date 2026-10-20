package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.Recall
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Recall — "Discard X cards, then return a card from your graveyard to your hand for each card
 * discarded this way. Exile Recall."
 *
 * Pins the X-linked discard, the graveyard being re-read after the discard (a just-discarded
 * card may come straight back), the return count following the cards actually discarded when
 * the hand holds fewer than X, and Recall exiling itself.
 */
class RecallScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Recall))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("discard X chosen cards, then return that many from the graveyard — a discarded card included") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.putCardInHand(me, "Grizzly Bears")
        val lions = d.putCardInHand(me, "Savannah Lions")
        val growth = d.putCardInGraveyard(me, "Giant Growth")
        val bolt = d.putCardInGraveyard(me, "Lightning Bolt")
        val recall = d.putCardInHand(me, "Recall")

        d.giveMana(me, Color.BLUE, 5)
        d.castXSpell(me, recall, xValue = 2).error shouldBe null
        d.bothPass()

        val discard = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        discard.minSelections shouldBe 2
        discard.maxSelections shouldBe 2
        d.submitCardSelection(me, listOf(bears, lions)).error shouldBe null

        val ret = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        ret.minSelections shouldBe 2
        ret.maxSelections shouldBe 2
        ret.options shouldContainAll listOf(bears, lions, growth, bolt)
        ret.options shouldNotContain recall
        d.submitCardSelection(me, listOf(bears, bolt)).error shouldBe null

        d.getHand(me) shouldContainAll listOf(bears, bolt)
        d.getHand(me) shouldNotContain lions
        d.getGraveyard(me) shouldContainAll listOf(lions, growth)
        d.getGraveyard(me) shouldNotContain recall
        d.getExile(me) shouldContain recall
    }

    test("with fewer than X cards in hand, the whole hand is discarded and only that many return") {
        val d = driver()
        val me = d.activePlayer!!
        val recall = d.putCardInHand(me, "Recall")
        val handSize = d.getHand(me).size - 1
        val growth = d.putCardInGraveyard(me, "Giant Growth")
        val bolt = d.putCardInGraveyard(me, "Lightning Bolt")

        val x = handSize + 2
        d.giveMana(me, Color.BLUE, 2 * x + 1)
        d.castXSpell(me, recall, xValue = x).error shouldBe null
        d.bothPass()

        // The whole hand goes without a prompt; the return asks for exactly handSize cards.
        val ret = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        ret.minSelections shouldBe handSize
        ret.maxSelections shouldBe handSize
        d.submitCardSelection(me, listOf(growth, bolt) + ret.options.filter { it != growth && it != bolt }.take(handSize - 2))
            .error shouldBe null

        d.getHand(me).size shouldBe handSize
        d.getHand(me) shouldContainAll listOf(growth, bolt)
        d.getExile(me) shouldContain recall
    }
})
