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

class BogBadgerScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("kicked grant includes the Badger and current allies, excludes enemies and later arrivals, and expires") {
        val d = driver()
        val ally = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val enemy = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val badger = d.putCardInHand(d.player1, "Bog Badger")
        repeat(3) { d.putLandOnBattlefield(d.player1, "Forest") }
        d.putLandOnBattlefield(d.player1, "Swamp")

        d.submit(CastSpell(
            playerId = d.player1,
            cardId = badger,
            declaredCostSlot = ChoiceSlot.KICKED,
            paymentStrategy = PaymentStrategy.AutoPay,
        )).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 1
        d.bothPass()

        d.state.projectedState.hasKeyword(badger, Keyword.MENACE) shouldBe true
        d.state.projectedState.hasKeyword(ally, Keyword.MENACE) shouldBe true
        d.state.projectedState.hasKeyword(enemy, Keyword.MENACE) shouldBe false
        val late = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.state.projectedState.hasKeyword(late, Keyword.MENACE) shouldBe false

        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.state.projectedState.hasKeyword(badger, Keyword.MENACE) shouldBe false
        d.state.projectedState.hasKeyword(ally, Keyword.MENACE) shouldBe false
    }

    test("unkicked entry creates no trigger and grants no menace") {
        val d = driver()
        val badger = d.putCardInHand(d.player1, "Bog Badger")
        repeat(3) { d.putLandOnBattlefield(d.player1, "Forest") }
        d.submit(CastSpell(
            playerId = d.player1,
            cardId = badger,
            paymentStrategy = PaymentStrategy.AutoPay,
        )).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 0
        d.state.projectedState.hasKeyword(badger, Keyword.MENACE) shouldBe false
    }
})
