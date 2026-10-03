package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Winter Moon — "Players can't untap more than one nonbasic land during their untap steps."
 */
class WinterMoonScenarioTest : FunSpec({

    val NonbasicLand = CardDefinition(
        name = "Test Nonbasic Land",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine.parse("Land"),
    )

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NonbasicLand))
        return d
    }

    test("only one nonbasic land untaps; basic lands untap freely") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(me, "Winter Moon")
        val n1 = d.putPermanentOnBattlefield(me, "Test Nonbasic Land")
        val n2 = d.putPermanentOnBattlefield(me, "Test Nonbasic Land")
        val n3 = d.putPermanentOnBattlefield(me, "Test Nonbasic Land")
        val b1 = d.putPermanentOnBattlefield(me, "Plains")
        val b2 = d.putPermanentOnBattlefield(me, "Plains")
        listOf(n1, n2, n3, b1, b2).forEach { d.tapPermanent(it) }

        d.passPriorityUntil(Step.UNTAP)
        val decision = d.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        decision.minSelections shouldBe 2
        decision.options.toSet() shouldBe setOf(n1, n2, n3)

        d.submitCardSelection(me, listOf(n1, n2))
        d.state.getEntity(n1)?.has<TappedComponent>() shouldBe true
        d.state.getEntity(n2)?.has<TappedComponent>() shouldBe true
        d.state.getEntity(n3)?.has<TappedComponent>() shouldBe false
        d.state.getEntity(b1)?.has<TappedComponent>() shouldBe false
        d.state.getEntity(b2)?.has<TappedComponent>() shouldBe false
    }

    test("a single tapped nonbasic land untaps without a decision") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(me, "Winter Moon")
        val n1 = d.putPermanentOnBattlefield(me, "Test Nonbasic Land")
        val b1 = d.putPermanentOnBattlefield(me, "Plains")
        d.tapPermanent(n1)
        d.tapPermanent(b1)

        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe me
        d.state.getEntity(n1)?.has<TappedComponent>() shouldBe false
        d.state.getEntity(b1)?.has<TappedComponent>() shouldBe false
    }
})
