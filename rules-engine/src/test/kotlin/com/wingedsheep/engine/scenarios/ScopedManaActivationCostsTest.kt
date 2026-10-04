package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor
import com.wingedsheep.engine.mechanics.mana.*
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScopedManaActivationCostsTest : FunSpec({
    val life = card("Scoped Life Producer") {
        typeLine = "Land"
        activatedAbility { cost = Costs.PayLife(2); effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
    }
    val sacrifice = card("Scoped Sacrifice Producer") {
        typeLine = "Snow Land"
        activatedAbility { cost = Costs.SacrificeSelf; effect = Effects.AddMana(Color.BLUE, 2); manaAbility = true }
    }
    val charge = card("Scoped Counter Producer") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.RemoveXCounters(CounterType.CHARGE, DynamicAmount.Fixed(1), self = true)
            effect = Effects.AddMana(Color.BLUE, 1); manaAbility = true
        }
    }
    val converter = card("Scoped Repeated Converter") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Mana("{G}"); effect = Effects.AddMana(Color.BLUE, 1); manaAbility = true }
    }
    val spell = card("Scoped Repeated Payment") {
        typeLine = "Sorcery"; manaCost = "{U}{U}"
        spell { effect = Effects.GainLife(1) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(life, sacrifice, charge, converter, spell))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scoped(d: GameTestDriver): GameState = d.state.pushContinuation(
        ManaSpendingObligationsContinuation(d.activePlayer!!,
            EffectContext(sourceId = null, controllerId = d.activePlayer!!), "cost-scope"))
    fun counters(d: GameTestDriver, source: com.wingedsheep.sdk.model.EntityId, amount: Int) {
        d.replaceState(d.state.updateEntity(source) { it.with(CountersComponent(mapOf(CounterType.CHARGE to amount))) })
    }
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun pay(d: GameTestDriver, state: GameState, cost: String,
        strategy: PaymentStrategy = PaymentStrategy.AutoPay) =
        CastPaymentProcessor(d.services.zones, d.services.manaSolver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor).processPayment(state,
            CastSpell(d.activePlayer!!, d.activePlayer!!, paymentStrategy = strategy),
            ManaCost.parse(cost), "Scoped costs", 0, context)
    fun assertRejected(d: GameTestDriver, state: GameState, cost: String) {
        d.services.manaSolver.canPay(state, d.activePlayer!!, ManaCost.parse(cost), spellContext = context) shouldBe false
        val result = pay(d, state, cost)
        result.error.isNullOrEmpty() shouldBe false
        result.state shouldBe state
        result.events shouldBe emptyList()
    }

    test("repeated life payments use one source and one shared life budget") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, life.name); d.setLifeTotal(p, 5)
        val s = scoped(d)
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{G}{G}"), spellContext = context) shouldBe true
        s.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 5
        val r = pay(d, s, "{G}{G}")
        r.error shouldBe null
        r.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 1
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(source, source)
        r.state.remainingManaObligations(p) shouldBe false
        r.state.getEntity(source)!!.has<TappedComponent>() shouldBe false
    }
    test("two life sources cannot spend the same life twice") {
        val d = driver(); val p = d.activePlayer!!
        repeat(2) { d.putLandOnBattlefield(p, life.name) }; d.setLifeTotal(p, 3)
        assertRejected(d, scoped(d), "{G}{G}")
    }
    test("mana activation and Phyrexian pip cannot spend the same life") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, life.name); d.setLifeTotal(p, 3)
        assertRejected(d, scoped(d), "{G}{U/P}")
    }
    test("ordinary mana payment reserves no life even at a negative life total") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, "Forest"); d.setLifeTotal(p, -1)
        val s = scoped(d)
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{G}"), spellContext = context) shouldBe true
        val r = pay(d, s, "{G}")
        r.error shouldBe null
        r.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe -1
        r.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
        r.phyrexianLifePips shouldBe 0
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("mana activation and Phyrexian pip may together spend exactly the remaining life") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, life.name); d.setLifeTotal(p, 4)
        val s = scoped(d)
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{G}{U/P}"), spellContext = context) shouldBe true
        val r = pay(d, s, "{G}{U/P}")
        r.error shouldBe null
        r.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 0
        r.phyrexianLifePips shouldBe 1
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(source)
        r.state.remainingManaObligations(p) shouldBe false
        s.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 4
    }
    test("Phyrexian payment finds a counter source after declining a life consuming source") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Green Counter Producer") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.RemoveXCounters(CounterType.CHARGE, DynamicAmount.Fixed(1), self = true)
                effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true
            }
        }
        d.registerCards(listOf(producer))
        val lifeSource = d.putLandOnBattlefield(p, life.name)
        val counterSource = d.putLandOnBattlefield(p, producer.name)
        counters(d, counterSource, 1); d.setLifeTotal(p, 3)
        val s = scoped(d)
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{G}{U/P}"), spellContext = context) shouldBe true
        val r = pay(d, s, "{G}{U/P}")
        r.error shouldBe null
        r.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 1
        r.state.getEntity(counterSource)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 0
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(counterSource)
        r.state.getEntity(lifeSource)!!.has<TappedComponent>() shouldBe false
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("forced explicit Phyrexian payment reserves life within the selected sources") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Explicit Green Counter Producer") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.RemoveXCounters(CounterType.CHARGE, DynamicAmount.Fixed(1), self = true)
                effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true
            }
        }
        val paid = card("Scoped Phyrexian Payment") {
            typeLine = "Sorcery"; manaCost = "{G}{U/P}"
            spell { effect = Effects.GainLife(1) }
        }
        d.registerCards(listOf(producer, paid))
        val lifeSource = d.putLandOnBattlefield(p, life.name)
        val counterSource = d.putLandOnBattlefield(p, producer.name)
        counters(d, counterSource, 1); d.setLifeTotal(p, 3)
        val id = d.putCardInHand(p, paid.name)
        val forced = d.services.effectExecutorRegistry.execute(scoped(d), Effects.ForcePlay("chosen"),
            EffectContext(sourceId = null, controllerId = p,
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        d.replaceState(forced.state)
        val question = d.pendingDecision as PlayCardDecision
        val before = d.state
        val rejected = d.submitDecision(p, PlayCardResponse(question.id, CastSpell(p, id,
            paymentStrategy = PaymentStrategy.Explicit(listOf(lifeSource), phyrexianLifePayments = listOf(Color.BLUE)))))
        rejected.error.isNullOrEmpty() shouldBe false
        d.state shouldBe before
        rejected.events shouldBe emptyList()
        val accepted = d.submitDecision(p, PlayCardResponse(question.id, CastSpell(p, id,
            paymentStrategy = PaymentStrategy.Explicit(listOf(counterSource), phyrexianLifePayments = listOf(Color.BLUE)))))
        accepted.error shouldBe null
        (id in d.state.stack) shouldBe true
        d.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 1
        d.state.getEntity(counterSource)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 0
        d.state.remainingManaObligations(p) shouldBe false
        d.state.pendingDecision shouldBe null
    }
    test("self sacrifice keeps excess snow provenance after the source leaves") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, sacrifice.name)
        val s = scoped(d); val r = pay(d, s, "{U}")
        r.error shouldBe null
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).contains(source) shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
        val extra = r.state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana.single()
        extra.obligationIds shouldBe emptySet()
        extra.source!!.sourceId shouldBe source
        extra.source.isSnow shouldBe true
        s.getBattlefield().contains(source) shouldBe true
    }
    test("sacrificed source cannot produce a second batch") {
        val d = driver(); d.putLandOnBattlefield(d.activePlayer!!, sacrifice.name)
        assertRejected(d, scoped(d), "{U}{U}{U}")
    }
    test("failed sacrifice branch publishes neither cost nor output") {
        val d = driver(); d.putLandOnBattlefield(d.activePlayer!!, sacrifice.name)
        assertRejected(d, scoped(d), "{G}")
    }
    test("fixed self counters can fund two distinct contributing activations") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, charge.name); counters(d, source, 2)
        val s = scoped(d); val r = pay(d, s, "{U}{U}")
        r.error shouldBe null
        r.state.getEntity(source)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 0
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 2
        r.state.remainingManaObligations(p) shouldBe false
        s.getEntity(source)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 2
    }
    test("counter exhaustion rejects a larger payment atomically") {
        val d = driver(); val source = d.putLandOnBattlefield(d.activePlayer!!, charge.name)
        counters(d, source, 1); assertRejected(d, scoped(d), "{U}{U}")
    }
    test("repeated fixed mana costs settle each feeder and each converter activation") {
        val d = driver(); val p = d.activePlayer!!
        repeat(2) { d.putLandOnBattlefield(p, "Forest") }
        val source = d.putLandOnBattlefield(p, converter.name)
        val r = pay(d, scoped(d), "{U}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().count { it.sourceId == source } shouldBe 2
        r.events.filterIsInstance<ManaSpentEvent>().size shouldBe 3
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("explicit source exclusions apply to life and sacrifice costs") {
        val d = driver(); val p = d.activePlayer!!
        val chosen = d.putLandOnBattlefield(p, life.name)
        d.putLandOnBattlefield(p, sacrifice.name)
        val s = scoped(d); val r = pay(d, s, "{U}", PaymentStrategy.Explicit(listOf(chosen)))
        r.error.isNullOrEmpty() shouldBe false; r.state shouldBe s; r.events shouldBe emptyList()
    }
    test("composite tap life and self sacrifice pays all costs once") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Composite Costs") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Composite(Costs.Tap, Costs.PayLife(2), Costs.SacrificeSelf)
                effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true
            }
        }
        d.registerCards(listOf(producer)); val source = d.putLandOnBattlefield(p, producer.name)
        d.setLifeTotal(p, 3)
        val r = pay(d, scoped(d), "{G}")
        r.error shouldBe null
        r.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 1
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).contains(source) shouldBe true
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
    }
    test("tap cost still prevents reusing a source with enough life") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Tap Life Costs") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Composite(Costs.Tap, Costs.PayLife(2))
                effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true
            }
        }
        d.registerCards(listOf(producer)); d.putLandOnBattlefield(p, producer.name)
        assertRejected(d, scoped(d), "{G}{G}")
    }
    test("mandatory paid play uses repeated counter activations through the existing decision") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, charge.name); counters(d, source, 2)
        val id = d.putCardInHand(p, spell.name)
        val forced = d.services.effectExecutorRegistry.execute(scoped(d), Effects.ForcePlay("chosen"),
            EffectContext(sourceId = null, controllerId = p,
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        d.replaceState(forced.state)
        val question = d.pendingDecision as PlayCardDecision
        d.submitDecision(p, PlayCardResponse(question.id, CastSpell(p, id))).error shouldBe null
        (id in d.state.stack) shouldBe true
        d.state.remainingManaObligations(p) shouldBe false
        d.state.pendingDecision shouldBe null
        d.state.getEntity(source)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 0
    }
    test("zero mana beside a tap cost remains a supported deterministic payment") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Zero Atom Costs") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Composite(Costs.Mana("{0}"), Costs.Tap)
                effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true
            }
        }
        d.registerCards(listOf(producer)); val source = d.putLandOnBattlefield(p, producer.name)
        val r = pay(d, scoped(d), "{G}")
        r.error shouldBe null
        r.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("paying exactly the remaining life is legal before state based actions") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, life.name); d.setLifeTotal(p, 2)
        val r = pay(d, scoped(d), "{G}")
        r.error shouldBe null
        r.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 0
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("activation restrictions still cap repeated non tap production") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Exhaust Cost") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.PayLife(1); effect = Effects.AddMana(Color.GREEN, 1)
                manaAbility = true; isExhaust = true
            }
        }
        d.registerCards(listOf(producer)); d.putLandOnBattlefield(p, producer.name)
        assertRejected(d, scoped(d), "{G}{G}")
    }
    test("a tapped source can then use its separate self sacrifice mana ability") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Two Ability Source") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
            activatedAbility { cost = Costs.SacrificeSelf; effect = Effects.AddMana(Color.BLUE, 1); manaAbility = true }
        }
        d.registerCards(listOf(producer)); val source = d.putLandOnBattlefield(p, producer.name)
        val r = pay(d, scoped(d), "{G}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(source, source)
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.costsTap } shouldBe listOf(true, false)
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).contains(source) shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("positive production loops are bounded without publishing speculative costs") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Loop Costs") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Mana("{G}"); effect = Effects.AddMana(Color.GREEN, 2); manaAbility = true }
        }
        d.registerCards(listOf(producer)); val source = d.putLandOnBattlefield(p, producer.name)
        val s = scoped(d).updateEntity(p) { it.with(ManaPoolComponent(green = 1)) }
        ScopedManaActivationPlanner(d.services, nodeLimit = 4).plan(s, p, ManaCost.parse("{U}"), context) shouldBe ScopedManaPlanResult.Unknown(setOf(ScopedManaSearchLimit.NODE_BUDGET))
        s.getEntity(p)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        s.getEntity(source)!!.has<TappedComponent>() shouldBe false
    }
})
