package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Imi Statue — "Players can't untap more than one artifact during their untap steps."
 *
 * The Statue is itself an artifact, so a tapped Statue counts against the cap.
 */
class ImiStatueScenarioTest : FunSpec({

    val Widget = CardDefinition.artifact(name = "Test Widget", manaCost = ManaCost.parse("{1}"))

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(Widget))
        return d
    }

    test("a tapped Imi Statue and two tapped artifacts: only one of the three untaps") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val statue = d.putPermanentOnBattlefield(me, "Imi Statue")
        val a1 = d.putPermanentOnBattlefield(me, "Test Widget")
        val a2 = d.putPermanentOnBattlefield(me, "Test Widget")
        listOf(statue, a1, a2).forEach { d.tapPermanent(it) }

        d.passPriorityUntil(Step.UNTAP)
        val decision = d.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        decision.minSelections shouldBe 2

        // Keep the two Widgets tapped; the Statue untaps.
        d.submitCardSelection(me, listOf(a1, a2))
        d.state.getEntity(a1)?.has<TappedComponent>() shouldBe true
        d.state.getEntity(a2)?.has<TappedComponent>() shouldBe true
        d.state.getEntity(statue)?.has<TappedComponent>() shouldBe false
    }

    test("a single tapped artifact untaps freely") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(me, "Imi Statue")
        val a1 = d.putPermanentOnBattlefield(me, "Test Widget")
        d.tapPermanent(a1)

        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe me
        d.state.getEntity(a1)?.has<TappedComponent>() shouldBe false
    }
})
