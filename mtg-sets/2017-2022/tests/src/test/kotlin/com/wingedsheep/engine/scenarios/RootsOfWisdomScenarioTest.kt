package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.RootsOfWisdom
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Roots of Wisdom: "Mill three cards, then return a land card or Elf card from your graveyard to
 * your hand. If you can't, draw a card."
 */
class RootsOfWisdomScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(RootsOfWisdom))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
    }

    fun GameTestDriver.resolve() {
        while (pendingDecision == null && stackSize > 0) bothPass()
    }

    test("returns a just-milled land or Elf card (mandatory) and draws nothing") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val third = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val second = d.putCardOnTopOfLibrary(you, "Llanowar Elves")
        val first = d.putCardOnTopOfLibrary(you, "Forest")

        d.giveMana(you, Color.GREEN, 2)
        val roots = d.putCardInHand(you, "Roots of Wisdom")
        val handBefore = d.getHandSize(you)
        d.castSpell(you, roots).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision as SelectCardsDecision
        decision.minSelections shouldBe 1
        decision.maxSelections shouldBe 1
        decision.options shouldContainExactlyInAnyOrder listOf(first, second)

        d.submitCardSelection(you, listOf(second)).error shouldBe null
        d.resolve()

        // Roots left the hand, the Elf came back, no card was drawn.
        d.getHandSize(you) shouldBe handBefore
        d.getHand(you) shouldContain second
        d.getGraveyard(you) shouldContain first
        d.getGraveyard(you) shouldContain third
    }

    test("with no land or Elf card in the graveyard, draws a card instead") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val milled = listOf(
            d.putCardOnTopOfLibrary(you, "Grizzly Bears"),
            d.putCardOnTopOfLibrary(you, "Hill Giant"),
            d.putCardOnTopOfLibrary(you, "Grizzly Bears"),
        )

        d.giveMana(you, Color.GREEN, 2)
        val roots = d.putCardInHand(you, "Roots of Wisdom")
        val handBefore = d.getHandSize(you)
        d.castSpell(you, roots).error shouldBe null
        d.bothPass()
        d.resolve()

        d.pendingDecision shouldBe null
        // Roots left the hand, one card was drawn.
        d.getHandSize(you) shouldBe handBefore
        milled.forEach { d.getGraveyard(you) shouldContain it }
    }
})
