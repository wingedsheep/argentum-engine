package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.Electrozoa
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Electrozoa (MH3) — "Flash / Flying / When this creature enters, you get {E}{E}. / At the beginning
 * of your first main phase, tap this creature unless you pay {E}."
 */
class ElectrozoaScenarioTest : FunSpec({
    /** Player1's turn, stopped in upkeep — before the first-main trigger fires. */
    fun driverAtUpkeep(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + Electrozoa)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.UPKEEP)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })

    test("entering gets you two energy") {
        val d = driverAtUpkeep()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val card = d.putCardInHand(d.player1, "Electrozoa")
        d.giveMana(d.player1, Color.BLUE, 3)
        d.castSpell(d.player1, card).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.energy() shouldBe 2
    }

    test("paying one energy at the first main phase keeps it untapped") {
        val d = driverAtUpkeep()
        val zoa = d.putCreatureOnBattlefield(d.player1, "Electrozoa")
        d.giveEnergy(2)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.bothPass()
        val question = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        question.playerId shouldBe d.player1
        d.submitDecision(d.player1, YesNoResponse(question.id, true)).error shouldBe null
        d.energy() shouldBe 1
        d.isTapped(zoa) shouldBe false
    }

    test("declining to pay taps it and keeps the energy") {
        val d = driverAtUpkeep()
        val zoa = d.putCreatureOnBattlefield(d.player1, "Electrozoa")
        d.giveEnergy(2)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.bothPass()
        val question = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitDecision(d.player1, YesNoResponse(question.id, false)).error shouldBe null
        d.energy() shouldBe 2
        d.isTapped(zoa) shouldBe true
    }

    test("with no energy it is simply tapped") {
        val d = driverAtUpkeep()
        val zoa = d.putCreatureOnBattlefield(d.player1, "Electrozoa")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.bothPass()
        d.pendingDecision shouldBe null
        d.isTapped(zoa) shouldBe true
    }
})
