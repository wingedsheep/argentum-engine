package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.AngelsTomb
import com.wingedsheep.mtg.sets.definitions.emn.cards.GrappleWithThePast
import com.wingedsheep.mtg.sets.definitions.mid.cards.EccentricFarmer
import com.wingedsheep.mtg.sets.definitions.mid.cards.SiegeZombie
import com.wingedsheep.mtg.sets.definitions.zen.cards.BlazingTorch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/** Scenario tests for Grapple with the Past. */
class GrappleWithThePastScenarioTest : FunSpec({

    val batch = listOf(GrappleWithThePast, EccentricFarmer, SiegeZombie, AngelsTomb, BlazingTorch)
    val projector = StateProjector()

    fun setup(deck: Deck = Deck.of("Forest" to 40)): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + batch)
        initMirrorMatch(deck = deck, startingLife = 20, skipMulligans = true)
    }

    // ── Grapple with the Past ────────────────────────────────────────────────

    test("Grapple with the Past: mills three, then returns a just-milled creature card to hand") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Seed the top three so the mill is deterministic: two creatures and a land.
        val third = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val second = d.putCardOnTopOfLibrary(you, "Forest")
        val first = d.putCardOnTopOfLibrary(you, "Hill Giant")
        val milled = setOf(first, second, third)

        d.giveMana(you, Color.GREEN, 2)
        val grapple = d.putCardInHand(you, "Grapple with the Past")
        d.castSpell(you, grapple).error shouldBe null
        d.bothPass()

        // The three cards are milled *before* the choice, so all three are offered back.
        val decision = d.pendingDecision as SelectCardsDecision
        decision.maxSelections shouldBe 1
        decision.minSelections shouldBe 0
        milled.forEach { decision.options shouldContain it }

        d.submitCardSelection(you, listOf(first)).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        d.getHand(you) shouldContain first
        d.getGraveyard(you) shouldContain second
        d.getGraveyard(you) shouldContain third
    }

    test("Grapple with the Past: declining the return leaves all three milled cards in the graveyard") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val third = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val second = d.putCardOnTopOfLibrary(you, "Forest")
        val first = d.putCardOnTopOfLibrary(you, "Hill Giant")

        d.giveMana(you, Color.GREEN, 2)
        val grapple = d.putCardInHand(you, "Grapple with the Past")
        val handBefore = d.getHandSize(you)
        d.castSpell(you, grapple).error shouldBe null
        d.bothPass()

        // "you may" = choose zero.
        d.submitCardSelection(you, emptyList()).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        // Hand shrank by exactly the Grapple itself; every milled card stayed in the graveyard.
        d.getHandSize(you) shouldBe handBefore - 1
        listOf(first, second, third).forEach { d.getGraveyard(you) shouldContain it }
    }
})
