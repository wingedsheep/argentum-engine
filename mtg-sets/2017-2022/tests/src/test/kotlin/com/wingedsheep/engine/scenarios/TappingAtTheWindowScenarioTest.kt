package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.TappingAtTheWindow
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/** Scenario tests for Tapping at the Window. */
class TappingAtTheWindowScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(TappingAtTheWindow))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
    }

    test("Tapping at the Window: chosen creature goes to hand, the rest to the graveyard") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val third = d.putCardOnTopOfLibrary(you, "Forest")
        val second = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val first = d.putCardOnTopOfLibrary(you, "Mountain")

        d.giveMana(you, Color.GREEN, 2)
        val spell = d.putCardInHand(you, "Tapping at the Window")
        d.castSpell(you, spell).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision as SelectCardsDecision
        decision.maxSelections shouldBe 1
        decision.minSelections shouldBe 0

        d.submitCardSelection(you, listOf(second)).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        d.getHand(you) shouldContain second
        d.getGraveyard(you) shouldContain first
        d.getGraveyard(you) shouldContain third
        d.getGraveyard(you) shouldNotContain second
    }

    test("Tapping at the Window: declining puts all three cards into the graveyard") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val third = d.putCardOnTopOfLibrary(you, "Forest")
        val second = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val first = d.putCardOnTopOfLibrary(you, "Mountain")

        d.giveMana(you, Color.GREEN, 2)
        val spell = d.putCardInHand(you, "Tapping at the Window")
        d.castSpell(you, spell).error shouldBe null
        d.bothPass()

        d.submitCardSelection(you, emptyList()).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        listOf(first, second, third).forEach { d.getGraveyard(you) shouldContain it }
    }
})
