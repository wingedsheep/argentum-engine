package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.RampagingGrowth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Rampaging Growth — the land found by the search is the one that becomes a 4/3 Insect with reach
 * and haste until end of turn, and it stays a land.
 */
class RampagingGrowthScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(RampagingGrowth))
        initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20, skipMulligans = true)
    }

    test("the searched-out basic land enters and becomes a 4/3 Insect with reach and haste until end of turn") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = d.putCardOnTopOfLibrary(you, "Forest")
        d.giveMana(you, Color.GREEN, 4)
        val growth = d.putCardInHand(you, "Rampaging Growth")
        d.castSpell(you, growth).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision as SelectCardsDecision
        decision.options shouldContain forest
        d.submitCardSelection(you, listOf(forest)).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        d.state.getBattlefield() shouldContain forest
        val projected = d.state.projectedState
        projected.isCreature(forest) shouldBe true
        projected.hasType(forest, "LAND") shouldBe true
        projected.hasSubtype(forest, "Forest") shouldBe true
        projected.hasSubtype(forest, "Insect") shouldBe true
        projected.getPower(forest) shouldBe 4
        projected.getToughness(forest) shouldBe 3
        projected.hasKeyword(forest, Keyword.REACH) shouldBe true
        projected.hasKeyword(forest, Keyword.HASTE) shouldBe true

        // Until end of turn: on the next turn it is just a Forest again.
        d.passPriorityUntil(Step.UPKEEP)
        d.state.getBattlefield() shouldContain forest
        d.state.projectedState.isCreature(forest) shouldBe false
        d.state.projectedState.hasType(forest, "LAND") shouldBe true
    }

    test("finding nothing animates nothing") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val landsBefore = d.state.getBattlefield().count { d.state.projectedState.hasType(it, "LAND") }
        d.giveMana(you, Color.GREEN, 4)
        val growth = d.putCardInHand(you, "Rampaging Growth")
        d.castSpell(you, growth).error shouldBe null
        d.bothPass()

        // The library holds no basic land; decline (or skip) the search.
        while (d.pendingDecision is SelectCardsDecision) d.submitCardSelection(you, emptyList()).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        d.stackSize shouldBe 0
        d.state.getBattlefield().count { d.state.projectedState.hasType(it, "LAND") } shouldBe landsBefore
        d.getGraveyard(you) shouldContain growth
    }
})
