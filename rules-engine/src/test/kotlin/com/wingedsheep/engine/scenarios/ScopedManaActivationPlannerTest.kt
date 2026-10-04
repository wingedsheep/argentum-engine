package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor
import com.wingedsheep.engine.mechanics.mana.*
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScopedManaActivationPlannerTest : FunSpec({
    val converter = card("Tap Chain Converter") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.Mana("{G}"))
            effect = Effects.AddMana(Color.BLUE, 2)
            manaAbility = true
        }
    }
    val double = card("Tap Double Probe") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddColorlessMana(2); manaAbility = true }
    }
    val zero = card("Tap Zero Probe") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddColorlessMana(0); manaAbility = true }
    }
    val bonus = card("Tap Fixed Bonus Probe") {
        typeLine = "Enchantment"
        staticAbility { ability = AdditionalManaOnSourceTap(GameObjectFilter.Land, color = Color.BLUE) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(converter, double, zero, bonus))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scoped(d: GameTestDriver, pool: ManaPoolComponent = ManaPoolComponent(), pending: Set<String> = emptySet()): GameState {
        val p = d.activePlayer!!
        return d.state.updateEntity(p) { it.with(pool) }.pushContinuation(
            ManaSpendingObligationsContinuation(p, EffectContext(sourceId = null, controllerId = p), "scope", pending))
    }
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun pay(d: GameTestDriver, s: GameState, cost: String, strategy: PaymentStrategy = PaymentStrategy.AutoPay,
        x: Int = 0, xColors: Set<Color> = emptySet()) =
        CastPaymentProcessor(d.services.zones, d.services.manaSolver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor).processPayment(s,
            CastSpell(d.activePlayer!!, d.activePlayer!!, paymentStrategy = strategy),
            ManaCost.parse(cost), "Scoped probe", x, context, xColors)

    test("paid tap chain discharges its feeder during activation and converter during casting") {
        val d = driver(); val p = d.activePlayer!!
        val feeder = d.putLandOnBattlefield(p, "Forest")
        val sink = d.putLandOnBattlefield(p, converter.name)
        val s = scoped(d)
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{U}"), spellContext = context) shouldBe true
        s.getEntity(feeder)!!.has<TappedComponent>() shouldBe false
        val r = pay(d, s, "{U}")
        r.error shouldBe null
        r.state.remainingManaObligations(p) shouldBe false
        r.state.getEntity(feeder)!!.has<TappedComponent>() shouldBe true
        r.state.getEntity(sink)!!.has<TappedComponent>() shouldBe true
        val pool = r.state.getEntity(p)!!.get<ManaPoolComponent>()!!
        pool.restrictedMana.single().color shouldBe Color.BLUE
        pool.restrictedMana.single().obligationIds shouldBe emptySet()
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 2
        r.events.filterIsInstance<ManaSpentEvent>().size shouldBe 2
    }
    test("a fixed tap bonus can help pay but cannot substitute for its base contribution") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, "Forest")
        d.putPermanentOnBattlefield(p, bonus.name)
        val s = scoped(d)
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{G}{U}"), spellContext = context) shouldBe true
        pay(d, s, "{G}{U}").error shouldBe null
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{U}"), spellContext = context) shouldBe false
        val failure = pay(d, s, "{U}")
        failure.error.isNullOrEmpty() shouldBe false
        failure.state shouldBe s
        failure.events shouldBe emptyList()
        s.getEntity(source)!!.has<TappedComponent>() shouldBe false
    }
    test("automatic excess keeps exact provenance and emits one activation") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, double.name)
        val r = pay(d, scoped(d), "{1}")
        r.error shouldBe null
        val unit = r.state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana.single()
        unit.source!!.sourceId shouldBe source
        unit.obligationIds shouldBe emptySet()
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
        r.events.filterIsInstance<ManaAddedEvent>().single().colorless shouldBe 2
    }
    test("existing manual wrong-color obligation rejects all hypothetical taps atomically") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest")
        val s = scoped(d, ManaPoolComponent(restrictedMana = listOf(
            RestrictedManaEntry(Color.RED, ManaRestriction.AnySpend, obligationIds = setOf("unpaid")))), setOf("unpaid"))
        val r = pay(d, s, "{G}")
        r.error.isNullOrEmpty() shouldBe false
        r.state shouldBe s
        r.events shouldBe emptyList()
    }
    test("zero-output branch is discarded and cannot manufacture a contribution") {
        val d = driver(); val p = d.activePlayer!!
        val dead = d.putLandOnBattlefield(p, zero.name)
        d.putLandOnBattlefield(p, "Forest")
        val r = pay(d, scoped(d), "{G}")
        r.error shouldBe null
        r.state.getEntity(dead)!!.has<TappedComponent>() shouldBe false
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
    }
    test("explicit selection excludes an otherwise needed chain feeder") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest")
        val sink = d.putLandOnBattlefield(p, converter.name)
        val s = scoped(d)
        val r = pay(d, s, "{U}", PaymentStrategy.Explicit(listOf(sink)))
        r.error.isNullOrEmpty() shouldBe false
        r.state shouldBe s
        r.events shouldBe emptyList()
    }
    test("source restrictions use the scoped player's projected filter for every chain link") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest")
        d.putLandOnBattlefield(p, converter.name)
        val s = scoped(d).pushContinuation(ManaAbilitySourcesContinuation(p,
            GameObjectFilter.Land.withSubtype(Subtype.FOREST), EffectContext(sourceId = null, controllerId = p)))
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{U}"), spellContext = context) shouldBe false
        pay(d, s, "{U}").state shouldBe s
    }
    test("restricted X chooses actual matching colors and rejects a chain of the wrong color") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest")
        d.putLandOnBattlefield(p, converter.name)
        val s = scoped(d)
        val r = pay(d, s, "{X}", x = 1, xColors = setOf(Color.BLUE))
        r.error shouldBe null
        r.xManaSpentByColor shouldBe mapOf(Color.BLUE to 1)
        r.state.remainingManaObligations(p) shouldBe false
        pay(d, s, "{X}", x = 1, xColors = setOf(Color.RED)).error.isNullOrEmpty() shouldBe false
    }
    test("nested scopes settle the same planned activations without touching another player") {
        val d = driver(); val p = d.activePlayer!!; val other = d.getOpponent(p)
        d.putLandOnBattlefield(p, "Forest")
        val ctx = EffectContext(sourceId = null, controllerId = p)
        val s = scoped(d).pushContinuation(ManaSpendingObligationsContinuation(p, ctx, "inner"))
            .pushContinuation(ManaSpendingObligationsContinuation(other, ctx, "other", setOf("unpaid")))
        val r = pay(d, s, "{G}")
        r.error shouldBe null
        r.state.remainingManaObligations(p) shouldBe false
        r.state.activeManaSpendingScope(other)!!.pendingIds shouldBe setOf("unpaid")
    }
    test("chosen colors use real production and hybrid allocation") {
        val d = driver(); val p = d.activePlayer!!
        val flexible = card("Scoped Flexible Probe") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddAnyColorMana(); manaAbility = true }
        }
        d.registerCards(listOf(flexible)); d.putLandOnBattlefield(p, flexible.name)
        val s = scoped(d)
        val r = pay(d, s, "{G/U}")
        r.error shouldBe null
        r.events.filterIsInstance<ManaAddedEvent>().single().let { it.green + it.blue } shouldBe 1
        r.state.remainingManaObligations(p) shouldBe false
        pay(d, s, "{C}").error.isNullOrEmpty() shouldBe false
    }
    test("restricted production cannot pay an incompatible spell") {
        val d = driver(); val p = d.activePlayer!!
        val restricted = card("Scoped Restricted Probe") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN, 2, restriction = ManaRestriction.CreatureSpellsOnly)
                manaAbility = true
            }
        }
        d.registerCards(listOf(restricted)); d.putLandOnBattlefield(p, restricted.name)
        val s = scoped(d)
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{G}"), spellContext = context) shouldBe false
        pay(d, s, "{G}").state shouldBe s
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{G}"),
            spellContext = SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE))) shouldBe true
    }
    test("mandatory forced play can use a paid chain and survives the existing card decision") {
        val d = driver(); val p = d.activePlayer!!
        val paid = card("Scoped Forced Payment Probe") {
            typeLine = "Sorcery"; manaCost = "{U}"
            spell { effect = Effects.GainLife(1) }
        }
        d.registerCards(listOf(paid))
        d.putLandOnBattlefield(p, "Forest"); d.putLandOnBattlefield(p, converter.name)
        val id = d.putCardInHand(p, paid.name)
        val ctx = EffectContext(sourceId = null, controllerId = p,
            pipeline = com.wingedsheep.engine.handlers.PipelineState(storedCollections = mapOf("chosen" to listOf(id))))
        val forced = d.services.effectExecutorRegistry.execute(scoped(d), Effects.ForcePlay("chosen"), ctx)
        d.replaceState(forced.state)
        val question = d.pendingDecision as PlayCardDecision
        val before = d.state
        d.services.legalActionEnumerator.enumerate(d.state, p).any { it.affordable } shouldBe true
        d.state shouldBe before
        val r = d.submitDecision(p, PlayCardResponse(question.id, CastSpell(p, id)))
        r.error shouldBe null
        (id in d.state.stack) shouldBe true
        d.state.remainingManaObligations(p) shouldBe false
        d.state.pendingDecision shouldBe null
    }
    test("forced explicit casting validates the complete selected paid chain") {
        val d = driver(); val p = d.activePlayer!!
        val paid = card("Scoped Explicit Forced Probe") {
            typeLine = "Sorcery"; manaCost = "{U}"
            spell { effect = Effects.GainLife(1) }
        }
        d.registerCards(listOf(paid))
        val feeder = d.putLandOnBattlefield(p, "Forest")
        val sink = d.putLandOnBattlefield(p, converter.name)
        val id = d.putCardInHand(p, paid.name)
        val ctx = EffectContext(sourceId = null, controllerId = p,
            pipeline = com.wingedsheep.engine.handlers.PipelineState(storedCollections = mapOf("chosen" to listOf(id))))
        val forced = d.services.effectExecutorRegistry.execute(scoped(d), Effects.ForcePlay("chosen"), ctx)
        d.replaceState(forced.state)
        val question = d.pendingDecision as PlayCardDecision
        val before = d.state
        val rejected = d.submitDecision(p, PlayCardResponse(question.id,
            CastSpell(p, id, paymentStrategy = PaymentStrategy.Explicit(listOf(sink)))))
        rejected.error.isNullOrEmpty() shouldBe false
        d.state shouldBe before
        val accepted = d.submitDecision(p, PlayCardResponse(question.id,
            CastSpell(p, id, paymentStrategy = PaymentStrategy.Explicit(listOf(feeder, sink)))))
        accepted.error shouldBe null
        (id in d.state.stack) shouldBe true
        d.state.getEntity(feeder)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(sink)!!.has<TappedComponent>() shouldBe true
        d.state.remainingManaObligations(p) shouldBe false
        d.state.pendingDecision shouldBe null
    }
    test("a bonus that pauses is declined without exposing its taps or new question") {
        val d = driver(); val p = d.activePlayer!!
        val pausing = card("Scoped Pausing Bonus Probe") {
            typeLine = "Enchantment"
            staticAbility { ability = AdditionalManaOnSourceTap(GameObjectFilter.Land, color = Color.BLUE,
                rider = Effects.May(Effects.GainLife(1))) }
        }
        d.registerCards(listOf(pausing)); d.putPermanentOnBattlefield(p, pausing.name)
        val forest = d.putLandOnBattlefield(p, "Forest")
        val s = scoped(d)
        val r = pay(d, s, "{G}{U}")
        r.error.isNullOrEmpty() shouldBe false
        r.state shouldBe s
        r.events shouldBe emptyList()
        r.state.pendingDecision shouldBe null
        r.state.getEntity(forest)!!.has<TappedComponent>() shouldBe false
    }
    test("context-free exact payment spends unrestricted tags and preserves ineligible entries") {
        val d = driver(); val p = d.activePlayer!!
        val other = RestrictedManaEntry(Color.RED, ManaRestriction.CreatureSpellsOnly)
        val s = scoped(d, ManaPoolComponent(restrictedMana = listOf(
            RestrictedManaEntry(Color.GREEN, ManaRestriction.AnySpend, obligationIds = setOf("green")), other)), setOf("green"))
        val processor = CastPaymentProcessor(d.services.zones, d.services.manaSolver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor)
        val r = processor.processPayment(s, CastSpell(p, p), ManaCost.parse("{G}"), "Null context probe", 0)
        r.error shouldBe null
        r.state.remainingManaObligations(p) shouldBe false
        r.state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana shouldBe listOf(other)
    }
    test("a short proof wins before permutations of unrelated taps exhaust the budget") {
        val d = driver(); val p = d.activePlayer!!
        val unrelated = List(5) { d.putLandOnBattlefield(p, "Mountain") }
        val needed = d.putLandOnBattlefield(p, "Forest")
        val s = scoped(d)
        d.services.manaSolver.canPay(s, p, ManaCost.parse("{G}"), spellContext = context) shouldBe true
        val r = pay(d, s, "{G}")
        r.error shouldBe null
        r.state.getEntity(needed)!!.has<TappedComponent>() shouldBe true
        unrelated.forEach { r.state.getEntity(it)!!.has<TappedComponent>() shouldBe false }
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("budget exhaustion is a declined proof and leaves input immutable") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest")
        val s = scoped(d)
        ScopedManaActivationPlanner(d.services, nodeLimit = 1).plan(s, p, ManaCost.parse("{G}"), context) shouldBe ScopedManaPlanResult.Unknown(setOf(ScopedManaSearchLimit.NODE_BUDGET))
        s.remainingManaObligations(p) shouldBe false
    }
})
