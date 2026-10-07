package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dst.cards.TalonOfPain
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TalonOfPainScenarioTest : FunSpec({
    fun duel(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + TalonOfPain + Shatter)
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.charges(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 30) passPriority(state.priorityPlayerId!!)
        state.stack.isEmpty() shouldBe true
    }

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED)
        castSpell(caster, bolt, listOf(target)).outcome shouldBe Outcome.Done
        resolveStack()
    }

    fun GameTestDriver.charge(id: EntityId, count: Int) {
        replaceState(state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.CHARGE, count)) })
    }

    fun GameTestDriver.activate(id: EntityId, x: Int) = submit(
        ActivateAbility(player1, id, TalonOfPain.activatedAbilities[0].id,
            targets = listOf(ChosenTarget.Player(player2)), xValue = x)
    )

    test("a damage spell adds one charge counter regardless of damage amount") {
        val d = duel()
        val talon = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        d.bolt(d.player1, d.player2)
        d.getLifeTotal(d.player2) shouldBe 17
        d.charges(talon) shouldBe 1
    }

    test("damage to yourself and damage from opposing sources add nothing") {
        val d = duel()
        val talon = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        d.bolt(d.player1, d.player1)
        d.charges(talon) shouldBe 0
        d.passPriority(d.player1)
        d.bolt(d.player2, d.player2)
        d.charges(talon) shouldBe 0
    }

    test("paying X removes counters and taps the source without recharging itself but charges another Talon") {
        val d = duel()
        val talon = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        val other = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        d.charge(talon, 3)
        d.giveColorlessMana(d.player1, 2)
        d.activate(talon, 2).outcome shouldBe Outcome.Done
        d.charges(talon) shouldBe 1
        d.isTapped(talon) shouldBe true
        d.resolveStack()
        d.getLifeTotal(d.player2) shouldBe 18
        d.charges(talon) shouldBe 1
        d.charges(other) shouldBe 1
    }

    test("another Talon sees damage from the activated source after that source leaves the battlefield") {
        val d = duel()
        val talon = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        val other = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        d.charge(talon, 2)
        d.giveColorlessMana(d.player1, 2)
        d.activate(talon, 2).outcome shouldBe Outcome.Done
        d.passPriority(d.player1)
        val shatter = d.putCardInHand(d.player2, "Shatter")
        d.giveMana(d.player2, Color.RED, 2)
        d.castSpell(d.player2, shatter, listOf(talon)).outcome shouldBe Outcome.Done
        d.resolveStack()
        d.state.getBattlefield().contains(talon) shouldBe false
        d.getLifeTotal(d.player2) shouldBe 18
        d.charges(other) shouldBe 1
    }

    test("X is bounded by both mana and the source's charge counters") {
        val d = duel()
        val talon = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        d.charge(talon, 2)
        (d.activate(talon, 2).outcome is Outcome.Done) shouldBe false
        d.giveColorlessMana(d.player1, 3)
        (d.activate(talon, 3).outcome is Outcome.Done) shouldBe false
        d.charges(talon) shouldBe 2
        d.isTapped(talon) shouldBe false
    }

    test("X zero is legal and deals no damage or charge counters") {
        val d = duel()
        val talon = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        val other = d.putPermanentOnBattlefield(d.player1, "Talon of Pain")
        d.activate(talon, 0).outcome shouldBe Outcome.Done
        d.resolveStack()
        d.getLifeTotal(d.player2) shouldBe 20
        d.charges(other) shouldBe 0
        d.isTapped(talon) shouldBe true
    }
})
