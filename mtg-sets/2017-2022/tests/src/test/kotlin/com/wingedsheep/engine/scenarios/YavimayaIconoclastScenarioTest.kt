package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class YavimayaIconoclastScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("kicked entry grants only itself temporary stats and haste while trample persists") {
        val d = driver()
        val ally = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val iconoclast = d.putCardInHand(d.player1, "Yavimaya Iconoclast")
        repeat(2) { d.putLandOnBattlefield(d.player1, "Forest") }
        d.putLandOnBattlefield(d.player1, "Mountain")

        d.submit(CastSpell(
            playerId = d.player1,
            cardId = iconoclast,
            declaredCostSlot = ChoiceSlot.KICKED,
            paymentStrategy = PaymentStrategy.AutoPay,
        )).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 1
        d.state.projectedState.getPower(iconoclast) shouldBe 3
        d.state.projectedState.hasKeyword(iconoclast, Keyword.HASTE) shouldBe false
        d.bothPass()

        d.state.projectedState.getPower(iconoclast) shouldBe 4
        d.state.projectedState.getToughness(iconoclast) shouldBe 3
        d.state.projectedState.hasKeyword(iconoclast, Keyword.HASTE) shouldBe true
        d.state.projectedState.hasKeyword(iconoclast, Keyword.TRAMPLE) shouldBe true
        d.state.projectedState.getPower(ally) shouldBe 2
        d.state.projectedState.hasKeyword(ally, Keyword.HASTE) shouldBe false

        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.state.projectedState.getPower(iconoclast) shouldBe 3
        d.state.projectedState.getToughness(iconoclast) shouldBe 2
        d.state.projectedState.hasKeyword(iconoclast, Keyword.HASTE) shouldBe false
        d.state.projectedState.hasKeyword(iconoclast, Keyword.TRAMPLE) shouldBe true
    }

    test("unkicked entry creates no trigger and keeps its printed stats and trample") {
        val d = driver()
        val iconoclast = d.putCardInHand(d.player1, "Yavimaya Iconoclast")
        repeat(2) { d.putLandOnBattlefield(d.player1, "Forest") }
        d.submit(CastSpell(
            playerId = d.player1,
            cardId = iconoclast,
            paymentStrategy = PaymentStrategy.AutoPay,
        )).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 0
        d.state.projectedState.getPower(iconoclast) shouldBe 3
        d.state.projectedState.getToughness(iconoclast) shouldBe 2
        d.state.projectedState.hasKeyword(iconoclast, Keyword.HASTE) shouldBe false
        d.state.projectedState.hasKeyword(iconoclast, Keyword.TRAMPLE) shouldBe true
    }
})
