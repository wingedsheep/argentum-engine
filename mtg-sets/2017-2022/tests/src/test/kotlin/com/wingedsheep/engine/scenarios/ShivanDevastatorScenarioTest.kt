package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.core.Keyword

class ShivanDevastatorScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast X becomes entry counters and haste permits an immediate flying attack") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Shivan Devastator")
        repeat(5) { d.putLandOnBattlefield(d.player1, "Mountain") }
        d.submit(CastSpell(d.player1, card, xValue = 4,
            paymentStrategy = PaymentStrategy.AutoPay)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 0
        d.state.projectedState.getPower(card) shouldBe 4
        d.state.projectedState.getToughness(card) shouldBe 4
        d.state.projectedState.hasKeyword(card, Keyword.FLYING) shouldBe true
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(card), d.player2).outcome shouldBe Outcome.Done
    }

    test("X zero resolves without counters and dies as a zero toughness creature") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Shivan Devastator")
        d.putLandOnBattlefield(d.player1, "Mountain")
        d.submit(CastSpell(d.player1, card, xValue = 0,
            paymentStrategy = PaymentStrategy.AutoPay)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(d.player1).contains(card) shouldBe true
        d.stackSize shouldBe 0
    }
})
