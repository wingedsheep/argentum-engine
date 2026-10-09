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
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.mtg.sets.definitions.dmu.cards.TimelessLotus
import io.kotest.matchers.shouldNotBe

class TimelessLotusScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast Lotus enters tapped then produces all five colors immediately after untapping") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Timeless Lotus")
        repeat(5) { d.putLandOnBattlefield(d.player1, "Mountain") }
        d.castSpell(d.player1, card).outcome shouldBe Outcome.Done
        d.bothPass()
        d.isTapped(card) shouldBe true
        val activation = ActivateAbility(d.player1, card, TimelessLotus.activatedAbilities.single().id)
        d.submit(activation).outcome shouldNotBe Outcome.Done
        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player1)
        d.isTapped(card) shouldBe false
        d.submit(activation).outcome shouldBe Outcome.Done
        d.stackSize shouldBe 0
        d.isTapped(card) shouldBe true
        val pool = d.state.getEntity(d.player1)!!.get<ManaPoolComponent>()!!
        listOf(pool.white, pool.blue, pool.black, pool.red, pool.green, pool.colorless) shouldBe
            listOf(1, 1, 1, 1, 1, 0)
        d.submit(activation).outcome shouldNotBe Outcome.Done
    }

    test("auto payment consumes the five color composite as five mana") {
        val d = driver()
        val lotus = d.putPermanentOnBattlefield(d.player1, "Timeless Lotus")
        val second = d.putCardInHand(d.player1, "Timeless Lotus")
        d.submit(CastSpell(d.player1, second, paymentStrategy = PaymentStrategy.AutoPay))
            .outcome shouldBe Outcome.Done
        d.isTapped(lotus) shouldBe true
        val pool = d.state.getEntity(d.player1)!!.get<ManaPoolComponent>()!!
        listOf(pool.white, pool.blue, pool.black, pool.red, pool.green, pool.colorless) shouldBe
            listOf(0, 0, 0, 0, 0, 0)
    }
})
