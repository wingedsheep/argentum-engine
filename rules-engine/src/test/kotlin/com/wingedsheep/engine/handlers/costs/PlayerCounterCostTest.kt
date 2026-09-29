package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PlayerCounterCostTest : FunSpec({
    fun spender(name: String, cost: AbilityCost) = card(name) {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            this.cost = cost
            effect = Effects.GainLife(1)
        }
    }
    val fixed = spender("Fixed Spender", Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 2)))
    val variable = spender("Variable Spender", Costs.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue))
    val combined = spender("Combined Spender", Costs.Composite(
        Costs.PayPlayerCounters(CounterType.ENERGY, 2), Costs.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue)))
    val derived = card("Derived Spender") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.PayPlayerCounters(CounterType.ENERGY, CostAtom.CollectEvidence.TARGET_SUM)
            val creature = target(TargetFilter.Creature)
            effect = Effects.Tap(creature)
        }
    }
    val energySpell = card("Energy Spell") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.Composite(listOf(
            Costs.additional.PayPlayerCounters(CounterType.ENERGY, 1),
            Costs.additional.Composite(listOf(Costs.additional.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue)))
        )))
        spell { effect = Effects.GainLife(DynamicAmount.XValue) }
    }
    val fixedSpell = card("Combined Energy Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.PayPlayerCounters(CounterType.ENERGY, 2))
        additionalCost(Costs.additional.PayPlayerCounters(CounterType.ENERGY, 2))
        spell { effect = Effects.GainLife(1) }
    }
    val manaSource = card("Energy Mana Source") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
            manaAbility = true
            effect = Effects.AddColorlessMana(1)
        }
    }
    val poisonSpender = spender("Poison Spender", Costs.PayPlayerCounters(CounterType.POISON, 1))
    val defined = card("Defined Energy Spender") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue)
            xDefinedAs = DynamicAmount.Fixed(3)
            effect = Effects.GainLife(1)
        }
    }
    fun driver(): EnumerationTestDriver = EnumerationTestDriver().also {
        it.registerCards(TestCards.all + listOf(fixed, variable, combined, derived, energySpell, fixedSpell, manaSource, poisonSpender, defined))
        it.game.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.game.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun EnumerationTestDriver.setEnergy(player: EntityId, amount: Int) = game.replaceState(
        game.state.updateEntity(player) { it.with(CountersComponent(mapOf(CounterType.ENERGY to amount))) })
    fun EnumerationTestDriver.energy(player: EntityId): Int =
        game.state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    test("fixed costs spend the payer's counters before resolution and emit the removal") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, fixed.name)
        d.setEnergy(d.player1, 3)
        d.setEnergy(d.player2, 7)
        val result = d.game.submit(ActivateAbility(d.player1, source, fixed.activatedAbilities.single().id))
        result.error shouldBe null
        d.energy(d.player1) shouldBe 1
        d.energy(d.player2) shouldBe 7
        d.game.state.stack.size shouldBe 1
        result.events.filterIsInstance<CountersRemovedEvent>().single().amount shouldBe 2
        d.game.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
    }
    test("insufficient energy disables activation and a forged action cannot tap or partially pay") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, fixed.name)
        d.setEnergy(d.player1, 1)
        d.setEnergy(d.player2, 7)
        d.enumerateFor(d.player1).activatedAbilityActionsFor(source).none { it.affordable } shouldBe true
        val before = d.game.state
        val result = d.game.submit(ActivateAbility(d.player1, source, fixed.activatedAbilities.single().id))
        result.error shouldNotBe null
        d.game.state shouldBe before
    }
    test("X picker is bounded by player counters and X is paid on announcement") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, variable.name)
        d.setEnergy(d.player1, 4)
        val offer = d.enumerateFor(d.player1).activatedAbilityActionsFor(source).single()
        offer.hasXCost shouldBe true
        offer.maxAffordableX shouldBe 4
        d.game.submit(ActivateAbility(d.player1, source, variable.activatedAbilities.single().id, xValue = 3)).error shouldBe null
        d.energy(d.player1) shouldBe 1
    }
    test("zero X works without a counters component and negative or excessive X fails atomically") {
        for (x in listOf(0, -1, 1)) {
            val d = driver()
            val source = d.game.putPermanentOnBattlefield(d.player1, variable.name)
            val before = d.game.state
            val result = d.game.submit(ActivateAbility(d.player1, source, variable.activatedAbilities.single().id, xValue = x))
            if (x == 0) result.error shouldBe null
            else {
                result.error shouldNotBe null
                d.game.state shouldBe before
            }
            d.energy(d.player1) shouldBe 0
        }
    }
    test("X cap reserves fixed energy costs in the same activation") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, combined.name)
        d.setEnergy(d.player1, 4)
        d.enumerateFor(d.player1).activatedAbilityActionsFor(source).single().maxAffordableX shouldBe 2
        val before = d.game.state
        d.game.submit(ActivateAbility(d.player1, source, combined.activatedAbilities.single().id, xValue = 3)).error shouldNotBe null
        d.game.state shouldBe before
        d.game.submit(ActivateAbility(d.player1, source, combined.activatedAbilities.single().id, xValue = 2)).error shouldBe null
        d.energy(d.player1) shouldBe 0
    }
    test("target-derived price is charged from announced targets and fails without enough counters") {
        for (energy in listOf(1, 2)) {
            val d = driver()
            val source = d.game.putPermanentOnBattlefield(d.player1, derived.name)
            val bear = d.game.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            d.setEnergy(d.player1, energy)
            val before = d.game.state
            val result = d.game.submit(ActivateAbility(d.player1, source, derived.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(bear))))
            if (energy == 1) {
                result.error shouldNotBe null
                d.game.state shouldBe before
            } else {
                result.error shouldBe null
                d.energy(d.player1) shouldBe 0
            }
        }
    }

    test("spell additional X uses counter budget and pays even without a selection payload") {
        val d = driver()
        val spell = d.game.putCardInHand(d.player1, energySpell.name)
        d.setEnergy(d.player1, 4)
        d.game.giveColorlessMana(d.player1, 1)
        val offer = d.enumerateFor(d.player1).castActionsFor(energySpell.name).single()
        offer.hasXCost shouldBe true
        offer.maxAffordableX shouldBe 3
        d.game.submit(com.wingedsheep.engine.core.CastSpell(d.player1, spell, xValue = 3,
            paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool)).error shouldBe null
        d.energy(d.player1) shouldBe 0
        d.game.bothPass()
        d.game.getLifeTotal(d.player1) shouldBe 23
    }
    test("spell affordability reserves all fixed player-counter payments together") {
        val d = driver()
        val spell = d.game.putCardInHand(d.player1, fixedSpell.name)
        d.setEnergy(d.player1, 3)
        d.enumerateFor(d.player1).castActionsFor(fixedSpell.name).none { it.affordable } shouldBe true
        val before = d.game.state
        d.game.submit(com.wingedsheep.engine.core.CastSpell(d.player1, spell)).error shouldNotBe null
        d.game.state shouldBe before
        d.setEnergy(d.player1, 4)
        d.game.submit(com.wingedsheep.engine.core.CastSpell(d.player1, spell)).error shouldBe null
        d.energy(d.player1) shouldBe 0
    }
    test("manual mana abilities spend counters and resolve without the stack") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, manaSource.name)
        d.enumerateFor(d.player1).activatedAbilityActionsFor(source).none { it.affordable } shouldBe true
        d.setEnergy(d.player1, 1)
        d.game.submit(ActivateAbility(d.player1, source, manaSource.activatedAbilities.single().id)).error shouldBe null
        d.energy(d.player1) shouldBe 0
        d.game.state.stack.size shouldBe 0
        d.game.state.getEntity(d.player1)?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()?.colorless shouldBe 1
    }
    test("player-counter type is parameterized and energy cannot pay poison") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, poisonSpender.name)
        d.setEnergy(d.player1, 3)
        val action = ActivateAbility(d.player1, source, poisonSpender.activatedAbilities.single().id)
        d.game.submit(action).error shouldNotBe null
        d.game.replaceState(d.game.state.updateEntity(d.player1) {
            it.with(CountersComponent(mapOf(CounterType.ENERGY to 3, CounterType.POISON to 1)))
        })
        d.game.submit(action).error shouldBe null
        d.energy(d.player1) shouldBe 3
        d.game.state.getEntity(d.player1)?.get<CountersComponent>()?.getCount(CounterType.POISON) shouldBe 0
    }
    test("defined X prices counters without a picker and rejects a player's cheaper declaration") {
        val d = driver()
        val source = d.game.putPermanentOnBattlefield(d.player1, defined.name)
        d.setEnergy(d.player1, 2)
        d.enumerateFor(d.player1).activatedAbilityActionsFor(source).none { it.affordable } shouldBe true
        d.setEnergy(d.player1, 4)
        d.enumerateFor(d.player1).activatedAbilityActionsFor(source).single().hasXCost shouldBe false
        d.game.submit(ActivateAbility(d.player1, source, defined.activatedAbilities.single().id, xValue = 0)).error shouldBe null
        d.energy(d.player1) shouldBe 1
    }
})
