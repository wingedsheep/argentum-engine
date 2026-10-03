package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m19.cards.ElvishRejuvenator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Elvish Rejuvenator (M19) — When this creature enters, look at the top five cards of your library.
 * You may put a land card from among them onto the battlefield tapped. Put the rest on the bottom
 * of your library in a random order.
 */
class ElvishRejuvenatorScenarioTest : FunSpec({

    test("puts a land from the top five onto the battlefield tapped, the rest to the bottom") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + ElvishRejuvenator)
            initMirrorMatch(deck = Deck.of("Centaur Courser" to 40))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = d.activePlayer!!
        val forest = d.putCardOnTopOfLibrary(you, "Forest")
        val libraryBefore = d.state.getLibrary(you)
        val topFive = libraryBefore.take(5)
        topFive shouldContain forest

        val elf = d.putCardInHand(you, "Elvish Rejuvenator")
        d.giveMana(you, Color.GREEN, 3)
        d.castSpell(you, elf).error shouldBe null
        d.bothPass() // resolve the creature spell; ETB trigger goes on the stack
        d.bothPass() // resolve the trigger

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(you, listOf(forest)).error shouldBe null

        d.state.getBattlefield(you) shouldContain forest
        d.isTapped(forest) shouldBe true

        val libraryAfter = d.state.getLibrary(you)
        libraryAfter.size shouldBe libraryBefore.size - 1
        libraryAfter shouldNotContain forest
        // The other four looked-at cards are now the bottom four.
        libraryAfter.takeLast(4).toSet() shouldBe (topFive - forest).toSet()
    }

    test("may decline: nothing enters, all five go to the bottom") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + ElvishRejuvenator)
            initMirrorMatch(deck = Deck.of("Centaur Courser" to 40))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = d.activePlayer!!
        val forest = d.putCardOnTopOfLibrary(you, "Forest")
        val topFive = d.state.getLibrary(you).take(5)

        val elf = d.putCardInHand(you, "Elvish Rejuvenator")
        d.giveMana(you, Color.GREEN, 3)
        d.castSpell(you, elf).error shouldBe null
        d.bothPass()
        d.bothPass()

        d.submitCardSelection(you, emptyList()).error shouldBe null
        d.state.getBattlefield(you) shouldNotContain forest
        d.state.getLibrary(you).takeLast(5).toSet() shouldBe topFive.toSet()
    }
})
