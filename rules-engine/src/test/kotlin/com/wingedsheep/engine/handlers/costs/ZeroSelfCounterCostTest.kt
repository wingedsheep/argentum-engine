package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.CostPaymentChoices
import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.mechanics.cost.CostPaymentContext
import com.wingedsheep.engine.mechanics.cost.PaymentResult
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ZeroSelfCounterCostTest : FunSpec({
    val spender = card("Test Zero Counter Spender") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap,
                Costs.RemoveXCounters(counterType = CounterType.CHARGE, self = true))
            effect = Effects.GainLife(1)
        }
    }

    fun driver() = EnumerationTestDriver().also {
        it.registerCards(TestCards.all + spender)
        it.game.initMirrorMatch(Deck.of("Forest" to 20), startingPlayer = 0)
        it.game.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    // Missing, empty, and populated storage must all give the same zero-cost behavior.
    for ((label, counters) in listOf(
        "missing" to null,
        "empty" to CountersComponent(),
        "populated" to CountersComponent(mapOf(CounterType.CHARGE to 2)),
    )) {
        for (type in listOf(CounterType.CHARGE, null)) {
            for (count in listOf(DynamicAmount.Fixed(0), DynamicAmount.XValue)) {
                test("activation removes zero $type counters with $label storage and $count") {
                    val d = driver().game
                    val source = d.putPermanentOnBattlefield(d.player1, spender.name)
                    if (counters != null) d.replaceState(d.state.updateEntity(source) { it.with(counters) })
                    val before = d.state
                    val cost = AbilityCost.Atom(CostAtom.RemoveCounters(count = count, counterType = type, self = true))
                    val handler = d.services.costHandler
                    handler.canPayAbilityCost(before, cost, source, d.player1, ManaPool()) shouldBe true
                    val result = handler.payAbilityCost(before, cost, source, d.player1, ManaPool(),
                        CostPaymentChoices(xValue = 0))
                    result.success shouldBe true
                    result.newState shouldBe before
                    result.events shouldBe emptyList()
                }
            }
            test("resolution payment removes zero $type counters with $label storage") {
                val d = driver().game
                val source = d.putPermanentOnBattlefield(d.player1, spender.name)
                if (counters != null) d.replaceState(d.state.updateEntity(source) { it.with(counters) })
                val before = d.state
                val cost = PayCost.Atom(CostAtom.RemoveCounters(count = DynamicAmount.Fixed(0), counterType = type, self = true))
                val service = CostPaymentService(d.services)
                service.canAfford(before, d.player1, cost, source) shouldBe true
                val result = service.performPayment(before, d.player1, cost, source, emptyMap())
                result.success shouldBe true
                result.state shouldBe before
                result.events shouldBe emptyList()
            }
        }
    }

    test("fresh source offers X zero and activation still pays its tap cost") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, spender.name)
        val action = d.enumerateFor(d.player1).activatedAbilityActionsFor(source).single()
        action.hasXCost shouldBe true
        action.maxAffordableX shouldBe 0
        val result = d.game.submit(ActivateAbility(d.player1, source, spender.activatedAbilities[0].id, xValue = 0))
        result.outcome shouldBe Outcome.Done
        result.events.filterIsInstance<CountersRemovedEvent>() shouldBe emptyList()
        d.game.isTapped(source) shouldBe true
        d.enumerateFor(d.player1).activatedAbilityActionsFor(source).single().affordable shouldBe false
    }

    for (accept in listOf(true, false)) {
        test("zero resolution cost still asks the player - accept $accept") {
            val d = driver().game
            val source = d.putPermanentOnBattlefield(d.player1, spender.name)
            val service = CostPaymentService(d.services)
            val cost = PayCost.Atom(CostAtom.RemoveCounters(count = DynamicAmount.Fixed(0), counterType = CounterType.CHARGE, self = true))
            val pending = service.pay(d.state, d.player1, cost, source,
                CostPaymentContext(onPaid = Effects.GainLife(1))) as PaymentResult.Pending
            d.replaceState(pending.state)
            d.getLifeTotal(d.player1) shouldBe 20
            val result = d.submitYesNo(d.player1, accept)
            result.outcome shouldBe Outcome.Done
            result.events.filterIsInstance<CountersRemovedEvent>() shouldBe emptyList()
            d.getLifeTotal(d.player1) shouldBe if (accept) 21 else 20
            d.state.getEntity(source)?.get<CountersComponent>() shouldBe null
        }
    }

    test("X offer is capped by both counter supply and mana") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, spender.name)
        d.game.replaceState(d.game.state.updateEntity(source) {
            it.with(CountersComponent(mapOf(CounterType.CHARGE to 3)))
        })
        d.game.giveColorlessMana(d.player1, 2)
        d.enumerateFor(d.player1).activatedAbilityActionsFor(source).single().maxAffordableX shouldBe 2
        d.game.giveColorlessMana(d.player1, 5)
        d.enumerateFor(d.player1).activatedAbilityActionsFor(source).single().maxAffordableX shouldBe 3
    }

    test("positive self-removal still needs counters on both payment paths") {
        val d = driver().game
        val source = d.putPermanentOnBattlefield(d.player1, spender.name)
        val atom = CostAtom.RemoveCounters(count = DynamicAmount.Fixed(1), counterType = CounterType.CHARGE, self = true)
        val handler = d.services.costHandler
        val service = CostPaymentService(d.services)
        handler.canPayAbilityCost(d.state, AbilityCost.Atom(atom), source, d.player1, ManaPool()) shouldBe false
        handler.payAbilityCost(d.state, AbilityCost.Atom(atom), source, d.player1, ManaPool()).success shouldBe false
        service.canAfford(d.state, d.player1, PayCost.Atom(atom), source) shouldBe false
        service.performPayment(d.state, d.player1, PayCost.Atom(atom), source, emptyMap()).success shouldBe false

        d.replaceState(d.state.updateEntity(source) { it.with(CountersComponent(mapOf(CounterType.CHARGE to 2))) })
        val result = service.performPayment(d.state, d.player1, PayCost.Atom(atom), source, emptyMap())
        result.success shouldBe true
        result.state.getEntity(source)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 1
        result.events.filterIsInstance<CountersRemovedEvent>().single().amount shouldBe 1
    }
})
