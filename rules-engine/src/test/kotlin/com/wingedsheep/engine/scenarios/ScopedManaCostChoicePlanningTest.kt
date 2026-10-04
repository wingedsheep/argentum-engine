package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor
import com.wingedsheep.engine.mechanics.mana.*
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Engine-level coverage of the shared activation-choice planner, across several mana-cost shapes. */
class ScopedManaCostChoicePlanningTest : FunSpec({
    fun producer(name: String, cost: AbilityCost, amount: DynamicAmount = DynamicAmount.Fixed(1)) = card(name) {
        typeLine = "Land"
        activatedAbility { this.cost = cost; effect = Effects.AddMana(Color.BLUE, amount); manaAbility = true }
    }
    val sacrifice = producer("Choice Sacrifice", Costs.Sacrifice(GameObjectFilter.Creature))
    val variable = producer("Choice Variable", Costs.SacrificePermanents(GameObjectFilter.Creature), DynamicAmount.XValue)
    val tap = producer("Choice Tap", Costs.TapPermanents(1, GameObjectFilter.Creature))
    val tapX = producer("Choice Tap X", Costs.TapXPermanents(GameObjectFilter.Creature), DynamicAmount.XValue)
    val counters = producer("Choice Counter X", Costs.RemoveXCounters(CounterType.CHARGE, self = true), DynamicAmount.XValue)
    val manaX = producer("Choice Mana X", Costs.Composite(Costs.Mana("{X}"), Costs.Tap), DynamicAmount.XValue)
    val paid = card("Choice Paid Spell") {
        typeLine = "Sorcery"; manaCost = "{U}{U}"
        spell { effect = Effects.GainLife(1) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(sacrifice, variable, tap, tapX, counters, manaX, paid))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scoped(d: GameTestDriver) = d.state.pushContinuation(
        ManaSpendingObligationsContinuation(d.activePlayer!!,
            EffectContext(sourceId = null, controllerId = d.activePlayer!!), "cost-choice"))
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun plan(d: GameTestDriver, state: GameState, cost: String, budget: Int = 256,
             excluded: Set<EntityId> = emptySet()) = ScopedManaActivationPlanner(d.services, budget)
        .plan(state, d.activePlayer!!, ManaCost.parse(cost), context, excludeSources = excluded)
    fun pay(d: GameTestDriver, state: GameState, cost: String,
            strategy: PaymentStrategy = PaymentStrategy.AutoPay) =
        CastPaymentProcessor(d.services.zones, d.services.manaSolver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor).processPayment(state,
            CastSpell(d.activePlayer!!, d.activePlayer!!, paymentStrategy = strategy),
            ManaCost.parse(cost), "Cost choices", 0, context)
    fun creature(d: GameTestDriver) = d.putCreatureOnBattlefield(d.activePlayer!!, "Grizzly Bears")
    fun counter(d: GameTestDriver, id: EntityId, n: Int, type: CounterType = CounterType.CHARGE) {
        d.replaceState(d.state.updateEntity(id) { it.with(CountersComponent(mapOf(type to n))) })
    }

    test("fixed sacrifice choices pay repeated activations without reusing a creature") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, sacrifice.name)
        val victims = listOf(creature(d), creature(d)); val s = scoped(d)
        val r = pay(d, s, "{U}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(source, source)
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).containsAll(victims) shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
        s.getBattlefield().containsAll(victims) shouldBe true
    }
    test("sacrifice choice preserves the creature needed as a later mana source") {
        val d = driver(); val p = d.activePlayer!!
        val creatureMana = card("Choice Green Creature") {
            typeLine = "Creature — Bear"; power = 2; toughness = 2
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
        }
        d.registerCards(listOf(creatureMana))
        d.putLandOnBattlefield(p, sacrifice.name)
        val keep = d.putCreatureOnBattlefield(p, creatureMana.name)
        d.replaceState(d.state.updateEntity(keep) { it.without<SummoningSicknessComponent>() })
        val victim = creature(d)
        val r = pay(d, scoped(d), "{G}{U}")
        r.error shouldBe null
        r.state.getBattlefield().contains(keep) shouldBe true
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).contains(victim) shouldBe true
        r.state.getEntity(keep)!!.has<TappedComponent>() shouldBe true
    }
    test("failed sacrifice search publishes no choices or sacrifice events") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, sacrifice.name); val victim = creature(d); val s = scoped(d)
        val r = pay(d, s, "{U}{U}")
        r.error.isNullOrEmpty() shouldBe false; r.state shouldBe s; r.events shouldBe emptyList()
        s.getBattlefield().contains(victim) shouldBe true
    }
    test("variable sacrifice binds X to the selected count in one activation") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, variable.name)
        val victims = listOf(creature(d), creature(d)); val s = scoped(d)
        val r = pay(d, s, "{U}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(source)
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).containsAll(victims) shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("fixed other-permanent tap can use a summoning sick creature once") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, tap.name); val victim = creature(d); val s = scoped(d)
        val r = pay(d, s, "{U}")
        r.error shouldBe null
        r.state.getEntity(victim)!!.has<TappedComponent>() shouldBe true
        s.getEntity(victim)!!.has<TappedComponent>() shouldBe false
        val failed = pay(d, s, "{U}{U}")
        failed.error.isNullOrEmpty() shouldBe false; failed.state shouldBe s; failed.events shouldBe emptyList()
    }
    test("tap X traverses number and object questions and preserves the caller floor") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, tapX.name)
        val victims = listOf(creature(d), creature(d)); val s = scoped(d)
        val r = pay(d, s, "{U}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(source)
        victims.all { r.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
        r.state.continuationStack.size shouldBe s.continuationStack.size
        r.state.pendingDecision shouldBe null
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("named self-counter X chooses enough counters and leaves excess available") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, counters.name); counter(d, source, 3); val s = scoped(d)
        val r = pay(d, s, "{U}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
        r.state.getEntity(source)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 1
        s.getEntity(source)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 3
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("counter X cannot exceed its shared counter budget") {
        val d = driver(); val source = d.putLandOnBattlefield(d.activePlayer!!, counters.name)
        counter(d, source, 1); val s = scoped(d); val r = pay(d, s, "{U}{U}")
        r.error.isNullOrEmpty() shouldBe false; r.state shouldBe s; r.events shouldBe emptyList()
    }
    test("mana X converter pays the chosen amount from actual feeder mana") {
        val d = driver(); val p = d.activePlayer!!
        repeat(2) { d.putLandOnBattlefield(p, "Forest") }
        val source = d.putLandOnBattlefield(p, manaX.name); val s = scoped(d)
        val r = pay(d, s, "{U}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().count { it.sourceId == source } shouldBe 1
        r.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
        s.getEntity(source)!!.has<TappedComponent>() shouldBe false
    }
    test("cost object filters use projected creature characteristics") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, sacrifice.name)
        val victim = d.putLandOnBattlefield(p, "Forest")
        val animated = d.services.effectExecutorRegistry.execute(d.state, Effects.AnimateLand(
            power = 2, toughness = 2, target = com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(victim)),
            EffectContext(sourceId = null, controllerId = p))
        animated.error shouldBe null
        d.replaceState(animated.state)
        val r = pay(d, scoped(d), "{U}")
        r.error shouldBe null
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).contains(victim) shouldBe true
    }
    test("choice branches consume the same budget without publishing partial activations") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, variable.name); repeat(18) { creature(d) }; val s = scoped(d)
        val outcome = plan(d, s, "{G}", budget = 4) as ScopedManaPlanResult.Unknown
        outcome.reasons.contains(ScopedManaSearchLimit.NODE_BUDGET) shouldBe true
        s.pendingDecision shouldBe null; s.getZone(ZoneKey(p, Zone.GRAVEYARD)).size shouldBe 0
    }
    test("excluded sources stay excluded while eligible cost objects are separate") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, sacrifice.name); creature(d); val s = scoped(d)
        plan(d, s, "{U}", excluded = setOf(source)) shouldBe ScopedManaPlanResult.Impossible
        val r = pay(d, s, "{U}", PaymentStrategy.Explicit(listOf(source)))
        r.error shouldBe null
    }
    test("forced paid play completes public cost choices through the existing play decision") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, variable.name); repeat(2) { creature(d) }
        val id = d.putCardInHand(p, paid.name)
        val forced = d.services.effectExecutorRegistry.execute(scoped(d), Effects.ForcePlay("chosen"),
            EffectContext(sourceId = null, controllerId = p,
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        d.replaceState(forced.state); val question = d.pendingDecision as PlayCardDecision
        val r = d.submitDecision(p, PlayCardResponse(question.id, CastSpell(p, id)))
        r.error shouldBe null
        (id in d.state.stack) shouldBe true
        r.events.filterIsInstance<AbilityActivatedEvent>().count { it.sourceId == source } shouldBe 1
        d.state.pendingDecision shouldBe null
        d.state.remainingManaObligations(p) shouldBe false
    }
    test("variable tap measures its public selected set without sacrificing it") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = producer("Choice Variable Tap", Costs.TapPermanentsVariable(GameObjectFilter.Creature), DynamicAmount.XValue)
        d.registerCards(listOf(sourceCard)); d.putLandOnBattlefield(p, sourceCard.name)
        val victims = listOf(creature(d), creature(d)); val r = pay(d, scoped(d), "{U}{U}")
        r.error shouldBe null
        r.state.getBattlefield().containsAll(victims) shouldBe true
        victims.all { r.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("distinct-name selection explores past an invalid first pair") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = producer("Choice Different Names",
            Costs.SacrificeMultiple(2, GameObjectFilter.Creature, distinctNames = true))
        d.registerCards(listOf(sourceCard)); d.putLandOnBattlefield(p, sourceCard.name)
        repeat(2) { creature(d) }
        val other = d.putCreatureOnBattlefield(p, "Llanowar Elves")
        val r = pay(d, scoped(d), "{U}")
        r.error shouldBe null
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).contains(other) shouldBe true
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).size shouldBe 2
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("invalid distinct-name selections are budgeted instead of traversing a power set") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = producer("Choice Invalid Names",
            Costs.SacrificeMultiple(9, GameObjectFilter.Creature, distinctNames = true))
        d.registerCards(listOf(sourceCard)); d.putLandOnBattlefield(p, sourceCard.name)
        repeat(24) { creature(d) }
        repeat(8) { index ->
            val distinct = card("Choice Distinct $index") { typeLine = "Creature — Bear"; power = 2; toughness = 2 }
            d.registerCards(listOf(distinct)); d.putCreatureOnBattlefield(p, distinct.name)
        }
        val s = scoped(d)
        val result = plan(d, s, "{U}", budget = 4) as ScopedManaPlanResult.Unknown
        result.reasons.contains(ScopedManaSearchLimit.NODE_BUDGET) shouldBe true
        s.getZone(ZoneKey(p, Zone.GRAVEYARD)).size shouldBe 0
    }
    test("hidden-zone costs stay explicitly outside the public choice proof") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = producer("Choice Hidden Discard", Costs.DiscardCard)
        d.registerCards(listOf(sourceCard)); d.putLandOnBattlefield(p, sourceCard.name)
        d.putCardInHand(p, "Forest"); val s = scoped(d)
        val result = plan(d, s, "{U}") as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION)
        val r = pay(d, s, "{U}")
        r.error.isNullOrEmpty() shouldBe false; r.state shouldBe s; r.events shouldBe emptyList()
    }

    test("ordinary variable-cost mana activation resolves with its measured X") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, variable.name)
        val victims = listOf(creature(d), creature(d))
        val r = d.submit(ActivateAbility(p, source, variable.script.activatedAbilities.first().id,
            costPayment = AdditionalCostPayment(variableCostPermanents = victims)))
        r.error shouldBe null
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.blue shouldBe 2
        d.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).containsAll(victims) shouldBe true
    }
    test("ordinary mana X picker counts eligible restricted floating mana") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, manaX.name)
        d.replaceState(d.state.updateEntity(p) { it.with(ManaPoolComponent(restrictedMana =
            List(2) { RestrictedManaEntry(Color.GREEN, ManaRestriction.AnySpend) })) })
        d.submit(ActivateAbility(p, source, manaX.script.activatedAbilities.first().id)).error shouldBe null
        val question = d.pendingDecision as ChooseNumberDecision
        question.maxValue shouldBe 2
        d.submitDecision(p, NumberChosenResponse(question.id, 2)).error shouldBe null
        d.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.blue shouldBe 2
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana shouldBe emptyList()
    }

    listOf(sacrifice, variable, tap, tapX).forEach { costSource ->
        test("${costSource.name} cost objects follow projected control rather than ownership") {
            val d = driver(); val p = d.activePlayer!!; val opponent = if (d.player1 == p) d.player2 else d.player1
            d.putLandOnBattlefield(p, costSource.name)
            val ceded = creature(d)
            val received = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
            val target = com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(ceded)
            val give = d.services.effectExecutorRegistry.execute(d.state, Effects.GainControl(target),
                EffectContext(sourceId = null, controllerId = opponent))
            give.error shouldBe null; d.replaceState(give.state)
            val take = d.services.effectExecutorRegistry.execute(d.state, Effects.GainControl(
                com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(received)),
                EffectContext(sourceId = null, controllerId = p))
            take.error shouldBe null; d.replaceState(take.state)
            d.state.projectedState.getController(received) shouldBe p
            d.state.projectedState.getController(ceded) shouldBe opponent
            val before = scoped(d); val r = pay(d, before, "{U}")
            r.error shouldBe null
            r.state.getBattlefield().contains(ceded) shouldBe true
            r.state.getEntity(ceded)!!.has<TappedComponent>() shouldBe false
            if (costSource == sacrifice || costSource == variable) {
                r.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)).contains(received) shouldBe true
                r.events.filterIsInstance<PermanentsSacrificedEvent>().single().playerId shouldBe p
            } else {
                r.state.getEntity(received)!!.has<TappedComponent>() shouldBe true
            }
            r.state.remainingManaObligations(p) shouldBe false
            before.getBattlefield().contains(received) shouldBe true
        }
    }

    test("source-relative other-permanent tap cost uses the actual source in every cost query") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = producer("Choice Source Tap", Costs.TapPermanents(1, GameObjectFilter.Land.sourceItself()))
        d.registerCards(listOf(sourceCard)); val source = d.putLandOnBattlefield(p, sourceCard.name)
        val s = scoped(d); val r = pay(d, s, "{U}")
        r.error shouldBe null
        r.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
        s.getEntity(source)!!.has<TappedComponent>() shouldBe false
    }

    test("fixed tap choice uses the source's text-changed subtype") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = producer("Choice Changed Tap",
            Costs.TapPermanents(1, GameObjectFilter.Creature.withSubtype(Subtype("Elf"))))
        val goblin = card("Choice Goblin") { typeLine = "Creature — Goblin"; power = 1; toughness = 1 }
        d.registerCards(listOf(sourceCard, goblin))
        val source = d.putLandOnBattlefield(p, sourceCard.name)
        val victim = d.putCreatureOnBattlefield(p, goblin.name)
        d.replaceState(d.state.updateEntity(source) { it.with(
            TextReplacementComponent(listOf(
                TextReplacement("Elf", "Goblin",
                    TextReplacementCategory.CREATURE_TYPE)))) })
        val before = scoped(d); val r = pay(d, before, "{U}")
        r.error shouldBe null
        r.state.getEntity(victim)!!.has<TappedComponent>() shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
        before.getEntity(victim)!!.has<TappedComponent>() shouldBe false
    }

})
