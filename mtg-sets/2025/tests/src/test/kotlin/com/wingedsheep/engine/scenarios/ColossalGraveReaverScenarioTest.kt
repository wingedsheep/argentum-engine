package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdc.cards.ColossalGraveReaver
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Colossal Grave-Reaver (TDC #50) — "Whenever one or more creature cards are put into your
 * graveyard from your library, put one of them onto the battlefield."
 *
 * The batch trigger fires once for a whole mill (the card's ruling), and the payoff picks exactly
 * one card from the captured creature cards — not all of them (Hedge Shredder's "them") and not
 * any noncreature card milled alongside.
 */
class ColossalGraveReaverScenarioTest : FunSpec({

    val MillThree = card("Mill Three Iso") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Mill three cards."
        spell { effect = Patterns.Library.mill(3) }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + ColossalGraveReaver + MillThree)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
    }

    test("two milled creature cards trigger once; exactly the chosen one enters the battlefield") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(you, "Colossal Grave-Reaver")

        val land = d.putCardOnTopOfLibrary(you, "Forest")
        val bearA = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val bearB = d.putCardOnTopOfLibrary(you, "Grizzly Bears")

        val mill = d.putCardInHand(you, "Mill Three Iso")
        d.castSpell(you, mill)
        d.bothPass() // resolve the mill; the batch trigger goes on the stack
        d.stackSize shouldBe 1 // one trigger for the whole batch
        d.bothPass() // resolve the trigger

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options shouldContainExactlyInAnyOrder listOf(bearA, bearB)
        d.submitCardSelection(you, listOf(bearB))

        d.getPermanents(you).contains(bearB) shouldBe true
        d.getGraveyard(you).contains(bearA) shouldBe true
        d.getGraveyard(you).contains(land) shouldBe true
    }

    test("milling no creature cards does not trigger") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(you, "Colossal Grave-Reaver")

        repeat(3) { d.putCardOnTopOfLibrary(you, "Forest") }

        val mill = d.putCardInHand(you, "Mill Three Iso")
        d.castSpell(you, mill)
        d.bothPass()

        d.stackSize shouldBe 0
        d.getPermanents(you).size shouldBe 1
    }
})
