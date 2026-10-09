package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ShalaisAcolyte
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedPlains262
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ShalaisAcolyteScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(listOf(ShalaisAcolyte, DominariaUnitedPlains262, DominariaUnitedForest274))
        initMirrorMatch(deck = Deck.of("Plains" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("kicked Acolyte enters as a five six without a counter trigger and keeps its counters next turn") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Shalai's Acolyte")
        repeat(6) { d.putLandOnBattlefield(d.player1, "Plains") }
        d.putLandOnBattlefield(d.player1, "Forest")
        d.submit(CastSpell(d.player1, card, declaredCostSlot = ChoiceSlot.KICKED,
            paymentStrategy = PaymentStrategy.AutoPay)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 0
        d.state.projectedState.getPower(card) shouldBe 5
        d.state.projectedState.getToughness(card) shouldBe 6
        d.state.projectedState.hasKeyword(card, Keyword.FLYING) shouldBe true
        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.state.projectedState.getPower(card) shouldBe 5
        d.state.projectedState.getToughness(card) shouldBe 6
    }

    test("unkicked Acolyte enters with printed stats and flying") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Shalai's Acolyte")
        repeat(5) { d.putLandOnBattlefield(d.player1, "Plains") }
        d.submit(CastSpell(d.player1, card,
            paymentStrategy = PaymentStrategy.AutoPay)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 0
        d.state.projectedState.getPower(card) shouldBe 3
        d.state.projectedState.getToughness(card) shouldBe 4
        d.state.projectedState.hasKeyword(card, Keyword.FLYING) shouldBe true
    }
})
