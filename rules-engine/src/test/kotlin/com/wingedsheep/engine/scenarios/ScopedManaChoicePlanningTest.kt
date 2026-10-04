package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor
import com.wingedsheep.engine.mechanics.mana.*
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScopedManaChoicePlanningTest : FunSpec({
    val split = card("Scoped Planned Split") {
        typeLine = "Snow Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddDynamicMana(DynamicAmount.Fixed(3), setOf(Color.RED, Color.GREEN))
            manaAbility = true
        }
    }
    val pips = card("Scoped Planned Pips") {
        typeLine = "Snow Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddDynamicMana(DynamicAmount.Fixed(2), Color.entries.toSet())
            manaAbility = true
        }
    }
    val composite = card("Scoped Planned Composite") {
        typeLine = "Snow Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN, 1) then
                Effects.AddDynamicMana(DynamicAmount.Fixed(2), Color.entries.toSet()) then
                Effects.AddAnyColorMana(1)
            manaAbility = true
        }
    }
    val life = card("Scoped Planned Life Split") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.PayLife(2)
            effect = Effects.AddDynamicMana(DynamicAmount.Fixed(1), setOf(Color.RED, Color.GREEN))
            manaAbility = true
        }
    }
    val green = card("Scoped Planned Green") {
        typeLine = "Snow Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
    }
    val bonus = card("Scoped Planned Bonus") {
        typeLine = "Enchantment — Aura"
        staticAbility { ability = AdditionalManaOnTap(amount = DynamicAmount.Fixed(1), anyColor = true) }
    }
    val spell = card("Scoped Planned Forced Spell") {
        typeLine = "Sorcery"; manaCost = "{W}{U}"
        spell { effect = Effects.GainLife(1) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(split, pips, composite, life, green, bonus, spell))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scoped(d: GameTestDriver) = d.state.pushContinuation(ManaSpendingObligationsContinuation(
        d.activePlayer!!, EffectContext(sourceId = null, controllerId = d.activePlayer!!), "choice-scope"))
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun pool(state: GameState, d: GameTestDriver) = state.getEntity(d.activePlayer!!)!!.get<ManaPoolComponent>()!!
    fun pay(d: GameTestDriver, state: GameState, cost: String,
        strategy: PaymentStrategy = PaymentStrategy.AutoPay) =
        CastPaymentProcessor(d.services.zones, d.services.manaSolver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor).processPayment(state,
            CastSpell(d.activePlayer!!, d.activePlayer!!, paymentStrategy = strategy),
            ManaCost.parse(cost), "Scoped choices", 0, context)
    fun rejected(d: GameTestDriver, state: GameState, cost: String) {
        d.services.manaSolver.canPay(state, d.activePlayer!!, ManaCost.parse(cost), spellContext = context) shouldBe false
        val result = pay(d, state, cost)
        result.error.isNullOrEmpty() shouldBe false
        result.state shouldBe state
        result.events shouldBe emptyList()
    }
    fun verifyPaid(d: GameTestDriver, state: GameState, producer: CardDefinition, cost: String) {
        d.services.manaSolver.canPay(state, d.activePlayer!!, ManaCost.parse(cost), spellContext = context) shouldBe true
        val result = pay(d, state, cost)
        result.error shouldBe null
        result.state.pendingDecision shouldBe null
        result.state.remainingManaObligations(d.activePlayer!!) shouldBe false
        result.state.continuationStack.filterIsInstance<ScopedManaProductionContinuation>() shouldBe emptyList()
        result.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
        result.events.filterIsInstance<LandTappedForManaEvent>().size shouldBe 1
        result.events.filterIsInstance<ManaAddedEvent>().single { it.sourceName == producer.name }.total shouldBe ManaCost.parse(cost).cmc
    }

    test("number-split planning finds a mixed allocation without exposing a decision") {
        val d = driver(); val source = d.putLandOnBattlefield(d.activePlayer!!, split.name)
        val initial = scoped(d)
        verifyPaid(d, initial, split, "{R}{G}{G}")
        initial.getEntity(source)!!.has<TappedComponent>() shouldBe false
        pool(initial, d).restrictedMana shouldBe emptyList()
    }
    test("independent pip choices supply different required colors") {
        val d = driver(); d.putLandOnBattlefield(d.activePlayer!!, pips.name)
        verifyPaid(d, scoped(d), pips, "{W}{U}")
    }
    test("a dynamic pip pause resumes its following composite sibling") {
        val d = driver(); d.putLandOnBattlefield(d.activePlayer!!, composite.name)
        verifyPaid(d, scoped(d), composite, "{G}{W}{U}{B}")
    }
    test("composite choice leaves independently respect their disjoint color sets") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Planned Independent Siblings") {
            typeLine = "Snow Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.WHITE, Color.BLUE))) then
                    Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.RED, Color.GREEN)))
                manaAbility = true
            }
        }
        d.registerCards(listOf(producer)); d.putLandOnBattlefield(p, producer.name)
        val initial = scoped(d)
        verifyPaid(d, initial, producer, "{U}{G}")
        val paid = pay(d, initial, "{U}{G}")
        paid.events.filterIsInstance<ManaAddedEvent>().single { it.sourceName == producer.name }.let {
            it.blue shouldBe 1; it.green shouldBe 1
            it.white shouldBe 0; it.red shouldBe 0
        }
    }
    test("all resumed production shares one identity and preserves snow provenance") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, composite.name); val initial = scoped(d)
        val plan = (ScopedManaActivationPlanner(d.services).plan(initial, p, ManaCost.parse("{G}{W}{U}{B}"), context) as ScopedManaPlanResult.Found).execution
        val ids = plan.state.activeManaSpendingScope(p)!!.pendingIds
        ids.size shouldBe 1
        pool(plan.state, d).restrictedMana.size shouldBe 4
        pool(plan.state, d).restrictedMana.all {
            it.obligationIds == ids && it.source?.let { provenance ->
                provenance.sourceId == source && provenance.isSnow
            } == true
        } shouldBe true
        plan.state.pendingDecision shouldBe null
        plan.events.filterIsInstance<ManaAddedEvent>().single().total shouldBe 4
        initial.getEntity(source)!!.has<TappedComponent>() shouldBe false
        initial.activeManaSpendingScope(p)!!.pendingIds shouldBe emptySet()
    }
    test("automatic bonus color remains separate from primary scoped production") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, green.name)
        val aura = d.putPermanentOnBattlefield(p, bonus.name)
        d.replaceState(d.state.updateEntity(aura) { it.with(AttachedToComponent(source)) })
        val initial = scoped(d)
        val plan = (ScopedManaActivationPlanner(d.services).plan(initial, p, ManaCost.parse("{G}{U}"), context) as ScopedManaPlanResult.Found).execution
        val original = pool(plan.state, d).restrictedMana.single()
        original.color shouldBe Color.GREEN
        original.source!!.sourceId shouldBe source
        original.source.isSnow shouldBe true
        original.obligationIds shouldBe plan.state.activeManaSpendingScope(p)!!.pendingIds
        pool(plan.state, d).blue shouldBe 1
        plan.events.filterIsInstance<ManaAddedEvent>().size shouldBe 2
        plan.events.filterIsInstance<LandTappedForManaEvent>().size shouldBe 1
        val paid = pay(d, initial, "{G}{U}")
        paid.error shouldBe null
        paid.state.remainingManaObligations(p) shouldBe false
    }
    test("bonus-only payment cannot discharge the source production obligation") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, green.name)
        val aura = d.putPermanentOnBattlefield(p, bonus.name)
        d.replaceState(d.state.updateEntity(aura) { it.with(AttachedToComponent(source)) })
        rejected(d, scoped(d), "{U}")
    }
    test("failed speculative pip branches publish neither costs nor partially produced mana") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, pips.name); val initial = scoped(d)
        rejected(d, initial, "{W}{U}{B}")
        initial.getEntity(source)!!.has<TappedComponent>() shouldBe false
        pool(initial, d).restrictedMana shouldBe emptyList()
        initial.pendingDecision shouldBe null
    }
    test("a failed sacrificed split branch preserves its source and scope") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Planned Sacrifice Split") {
            typeLine = "Snow Land"
            activatedAbility {
                cost = Costs.SacrificeSelf
                effect = Effects.AddDynamicMana(DynamicAmount.Fixed(2), setOf(Color.RED, Color.GREEN))
                manaAbility = true
            }
        }
        d.registerCards(listOf(producer)); val source = d.putLandOnBattlefield(p, producer.name)
        val initial = scoped(d); rejected(d, initial, "{U}")
        (source in initial.getBattlefield()) shouldBe true
        initial.activeManaSpendingScope(p)!!.pendingIds shouldBe emptySet()
    }
    test("dynamic life-cost choices respect reserved Phyrexian life") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, life.name); d.setLifeTotal(p, 3)
        rejected(d, scoped(d), "{G}{U/P}")
        d.setLifeTotal(p, 4)
        val initial = scoped(d); val paid = pay(d, initial, "{G}{U/P}")
        paid.error shouldBe null
        paid.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 0
        paid.phyrexianLifePips shouldBe 1
        paid.state.remainingManaObligations(p) shouldBe false
        initial.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe 4
    }
    test("explicit source choices exclude an otherwise sufficient dynamic producer") {
        val d = driver(); val p = d.activePlayer!!
        val chosen = d.putLandOnBattlefield(p, green.name)
        val excluded = d.putLandOnBattlefield(p, pips.name); val initial = scoped(d)
        ScopedManaActivationPlanner(d.services).plan(initial, p, ManaCost.parse("{W}{U}"), context,
            excludeSources = setOf(excluded)) shouldBe ScopedManaPlanResult.Impossible
        val paid = pay(d, initial, "{W}{U}", PaymentStrategy.Explicit(listOf(chosen)))
        paid.error.isNullOrEmpty() shouldBe false
        paid.state shouldBe initial
        paid.events shouldBe emptyList()
    }
    test("choice resumption stops before unrelated caller effects") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, pips.name)
        val caller = EffectContinuation(listOf(Effects.GainLife(7), Effects.May(Effects.GainLife(1))),
            EffectContext(sourceId = null, controllerId = p))
        val initial = scoped(d).pushContinuation(caller)
        val plan = (ScopedManaActivationPlanner(d.services).plan(initial, p, ManaCost.parse("{W}{U}"), context) as ScopedManaPlanResult.Found).execution
        plan.state.continuationStack.last() shouldBe caller
        plan.state.continuationStack.size shouldBe initial.continuationStack.size
        plan.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life shouldBe initial.lifeTotal(p)
        plan.state.pendingDecision shouldBe null
        plan.events.filterIsInstance<LifeChangedEvent>() shouldBe emptyList()
    }
    test("exhausting the choice reply budget preserves all original state") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, pips.name); val initial = scoped(d)
        ScopedManaActivationPlanner(d.services, nodeLimit = 3).plan(initial, p,
            ManaCost.parse("{W}{U}"), context) shouldBe ScopedManaPlanResult.Unknown(setOf(ScopedManaSearchLimit.NODE_BUDGET))
        initial.getEntity(source)!!.has<TappedComponent>() shouldBe false
        pool(initial, d).restrictedMana shouldBe emptyList()
        initial.activeManaSpendingScope(p)!!.pendingIds shouldBe emptySet()
        initial.pendingDecision shouldBe null
    }
    test("dynamic production mixed with a nonmana effect is excluded") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Planned Nonmana Rider") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddDynamicMana(DynamicAmount.Fixed(2), Color.entries.toSet()) then Effects.GainLife(1)
                manaAbility = true
            }
        }
        d.registerCards(listOf(producer)); d.putLandOnBattlefield(p, producer.name)
        rejected(d, scoped(d), "{W}{U}")
    }
    test("mandatory play completes dynamic choices while preserving the play continuation") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, pips.name)
        val id = d.putCardInHand(p, spell.name)
        val forced = d.services.effectExecutorRegistry.execute(scoped(d), Effects.ForcePlay("chosen"),
            EffectContext(sourceId = null, controllerId = p,
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        d.replaceState(forced.state)
        val question = d.pendingDecision as PlayCardDecision
        val originalSuspension = d.state.continuationStack.last() as Suspension
        val plan = (ScopedManaActivationPlanner(d.services).plan(d.state, p, ManaCost.parse("{W}{U}"), context) as ScopedManaPlanResult.Found).execution
        plan.state.continuationStack.last() shouldBe originalSuspension
        plan.state.pendingDecision shouldBe question
        plan.state.continuationStack.size shouldBe d.state.continuationStack.size
        (id in plan.state.stack) shouldBe false
        val result = d.submitDecision(p, PlayCardResponse(question.id, CastSpell(p, id)))
        result.error shouldBe null
        (id in d.state.stack) shouldBe true
        d.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
        d.state.pendingDecision shouldBe null
        d.state.remainingManaObligations(p) shouldBe false
        result.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
        result.events.filterIsInstance<ManaAddedEvent>().single { it.sourceName == pips.name }.let {
            it.white shouldBe 1; it.blue shouldBe 1
        }
    }
    test("bounded resumption cannot answer a caller suspension at its own floor") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, pips.name); val id = d.putCardInHand(p, spell.name)
        val forced = d.services.effectExecutorRegistry.execute(scoped(d), Effects.ForcePlay("chosen"),
            EffectContext(sourceId = null, controllerId = p,
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        val initial = forced.state
        val question = initial.pendingDecision as PlayCardDecision
        val result = d.services.continuationHandler.resumeWithin(initial,
            PlayCardResponse(question.id, CastSpell(p, id)), initial.continuationStack.size)
        result.error.isNullOrEmpty() shouldBe false
        result.state shouldBe initial
        result.events shouldBe emptyList()
    }
})
