package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.BloodPrice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Blood Price (ZNR #93) — {3}{B} Sorcery.
 *
 *   Look at the top four cards of your library. Put two of them into your hand and the rest on
 *   the bottom of your library in any order. You lose 2 life.
 */
class BloodPriceScenarioTest : FunSpec({

    test("keeps two of the top four, bottoms the rest, and costs 2 life") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(BloodPrice))
        d.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!

        val c1 = d.putCardOnTopOfLibrary(you, "Island")
        val c2 = d.putCardOnTopOfLibrary(you, "Forest")
        val c3 = d.putCardOnTopOfLibrary(you, "Mountain")
        val c4 = d.putCardOnTopOfLibrary(you, "Plains")

        val spell = d.putCardInHand(you, "Blood Price")
        d.giveMana(you, Color.BLACK, 4)
        val lifeBefore = d.getLifeTotal(you)
        d.castSpell(you, spell).outcome shouldBe Outcome.Done
        d.bothPass()

        val select = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        select.options shouldContainExactlyInAnyOrder listOf(c1, c2, c3, c4)
        select.minSelections shouldBe 2
        select.maxSelections shouldBe 2
        d.submitCardSelection(you, listOf(c4, c2))

        val reorder = d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        reorder.cards shouldContainExactlyInAnyOrder listOf(c1, c3)
        d.submitOrderedResponse(you, listOf(c3, c1))

        val hand = d.state.getZone(ZoneKey(you, Zone.HAND))
        hand.contains(c4) shouldBe true
        hand.contains(c2) shouldBe true
        val library = d.state.getZone(ZoneKey(you, Zone.LIBRARY))
        library.takeLast(2) shouldContainExactlyInAnyOrder listOf(c1, c3)
        d.getLifeTotal(you) shouldBe lifeBefore - 2
    }
})
