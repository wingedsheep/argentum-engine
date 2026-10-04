package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor
import com.wingedsheep.engine.mechanics.mana.*
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Mechanic tests: public graveyard choices execute real costs before proving a scoped payment. */
class ScopedManaGraveyardCostPlanningTest : FunSpec({
    fun producer(name: String, cost: AbilityCost, amount: DynamicAmount = DynamicAmount.Fixed(1)) = card(name) {
        typeLine = "Land"
        activatedAbility { this.cost = cost; effect = Effects.AddMana(Color.BLUE, amount); manaAbility = true }
    }
    val fixed = producer("Graveyard Fixed", Costs.ExileFromGraveyard(1, GameObjectFilter.Creature))
    val variable = producer("Graveyard X", Costs.ExileXFromGraveyard(GameObjectFilter.Creature), DynamicAmount.XValue)
    val single = producer("Graveyard Single", Costs.ExileFromSingleGraveyard(2, GameObjectFilter.Creature))
    val paid = card("Graveyard Paid Spell") {
        typeLine = "Sorcery"; manaCost = "{U}{U}"
        spell { effect = Effects.GainLife(1) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(fixed, variable, single, paid))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scoped(d: GameTestDriver) = d.state.pushContinuation(ManaSpendingObligationsContinuation(
        d.activePlayer!!, EffectContext(sourceId = null, controllerId = d.activePlayer!!), "graveyard-cost"))
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun plan(d: GameTestDriver, state: GameState, cost: String, budget: Int = 256) =
        ScopedManaActivationPlanner(d.services, budget).plan(state, d.activePlayer!!, ManaCost.parse(cost), context)
    fun pay(d: GameTestDriver, state: GameState, cost: String, strategy: PaymentStrategy = PaymentStrategy.AutoPay) =
        CastPaymentProcessor(d.services.zones, d.services.manaSolver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor).processPayment(state,
            CastSpell(d.activePlayer!!, d.activePlayer!!, paymentStrategy = strategy),
            ManaCost.parse(cost), "Graveyard cost", 0, context)
    fun victims(d: GameTestDriver, n: Int, player: EntityId = d.activePlayer!!) =
        List(n) { d.putCardInGraveyard(player, "Grizzly Bears") }

    test("fixed exile costs can repeat without reusing graveyard cards") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, fixed.name); val cards = victims(d, 2); val before = scoped(d)
        val r = pay(d, before, "{U}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(source, source)
        r.state.getZone(ZoneKey(p, Zone.EXILE)).containsAll(cards) shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
        before.getZone(ZoneKey(p, Zone.GRAVEYARD)).containsAll(cards) shouldBe true
    }
    test("exile planning explores alternatives instead of consuming another ability's only card") {
        val d = driver(); val p = d.activePlayer!!
        val elf = card("Graveyard Elf") { typeLine = "Creature — Elf"; power = 1; toughness = 1 }
        val green = card("Graveyard Green") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.ExileFromGraveyard(1, GameObjectFilter.Creature.withSubtype(Subtype("Elf")))
                effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true
            }
        }
        d.registerCards(listOf(elf, green))
        d.putLandOnBattlefield(p, fixed.name); d.putLandOnBattlefield(p, green.name)
        val unique = d.putCardInGraveyard(p, elf.name); val other = victims(d, 1).single()
        val r = pay(d, scoped(d), "{U}{G}")
        r.error shouldBe null
        r.state.getZone(ZoneKey(p, Zone.EXILE)).containsAll(listOf(unique, other)) shouldBe true
        r.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 2
    }
    test("an insufficient graveyard rejects payment without publishing any prefix") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, fixed.name); victims(d, 1); val before = scoped(d)
        plan(d, before, "{U}{U}") shouldBe ScopedManaPlanResult.Impossible
        val r = pay(d, before, "{U}{U}")
        r.error.isNullOrEmpty() shouldBe false; r.state shouldBe before; r.events shouldBe emptyList()
    }
    test("fixed exile never takes a nonmatching or opposing graveyard card") {
        val d = driver(); val p = d.activePlayer!!; val opponent = if (p == d.player1) d.player2 else d.player1
        d.putLandOnBattlefield(p, fixed.name); d.putCardInGraveyard(p, "Forest"); victims(d, 2, opponent)
        val before = scoped(d)
        plan(d, before, "{U}") shouldBe ScopedManaPlanResult.Impossible
    }
    test("single graveyard payment may use an opponent's public graveyard") {
        val d = driver(); val p = d.activePlayer!!; val opponent = if (p == d.player1) d.player2 else d.player1
        d.putLandOnBattlefield(p, single.name); val cards = victims(d, 2, opponent); val before = scoped(d)
        val r = pay(d, before, "{U}")
        r.error shouldBe null
        r.state.getZone(ZoneKey(opponent, Zone.EXILE)).containsAll(cards) shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("single graveyard cost cannot combine cards from different graveyards") {
        val d = driver(); val p = d.activePlayer!!; val opponent = if (p == d.player1) d.player2 else d.player1
        d.putLandOnBattlefield(p, single.name); victims(d, 1); victims(d, 1, opponent)
        plan(d, scoped(d), "{U}") shouldBe ScopedManaPlanResult.Impossible
    }
    test("single graveyard choices reject cross-owner subsets and find a legal same-owner pair") {
        val d = driver(); val p = d.activePlayer!!; val opponent = if (p == d.player1) d.player2 else d.player1
        d.putLandOnBattlefield(p, single.name); val own = victims(d, 2); val other = victims(d, 2, opponent)
        val r = pay(d, scoped(d), "{U}{U}")
        r.error shouldBe null
        r.state.getZone(ZoneKey(p, Zone.EXILE)).containsAll(own) shouldBe true
        r.state.getZone(ZoneKey(opponent, Zone.EXILE)).containsAll(other) shouldBe true
    }
    test("graveyard X derives production from the chosen exile count") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, variable.name); val cards = victims(d, 3); val before = scoped(d)
        val r = pay(d, before, "{U}{U}")
        r.error shouldBe null
        r.events.filterIsInstance<AbilityActivatedEvent>().map { it.sourceId } shouldBe listOf(source)
        r.state.getZone(ZoneKey(p, Zone.EXILE)).size shouldBe 2
        r.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).size shouldBe 1
        r.state.remainingManaObligations(p) shouldBe false
        before.getZone(ZoneKey(p, Zone.GRAVEYARD)).containsAll(cards) shouldBe true
    }
    test("graveyard X choices retain the source for source-relative filters") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = card("Graveyard Relative X") {
            typeLine = "Creature — Bear"; power = 1; toughness = 1
            activatedAbility {
                cost = Costs.ExileXFromGraveyard(GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.SharesCreatureTypeWithSource)))
                effect = Effects.AddMana(Color.BLUE, DynamicAmount.XValue); manaAbility = true
            }
        }
        d.registerCards(listOf(sourceCard))
        d.putCreatureOnBattlefield(p, sourceCard.name)
        val cards = victims(d, 2)
        val r = pay(d, scoped(d), "{U}{U}")
        r.error shouldBe null
        r.state.getZone(ZoneKey(p, Zone.EXILE)).containsAll(cards) shouldBe true
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("zero exile X output cannot satisfy its activation obligation") {
        val d = driver(); val p = d.activePlayer!!
        val zero = producer("Graveyard Zero", Costs.Composite(Costs.Tap,
            Costs.ExileXFromGraveyard(GameObjectFilter.Creature)), DynamicAmount.Fixed(0))
        d.registerCards(listOf(zero)); d.putLandOnBattlefield(p, zero.name); victims(d, 1)
        plan(d, scoped(d), "{U}") shouldBe ScopedManaPlanResult.Impossible
    }
    test("mana X and graveyard X share one announced value and pay feeder obligations") {
        val d = driver(); val p = d.activePlayer!!
        val converter = producer("Graveyard Mana X", Costs.Composite(Costs.Tap, Costs.Mana("{X}"),
            Costs.ExileXFromGraveyard(GameObjectFilter.Creature)), DynamicAmount.XValue)
        d.registerCards(listOf(converter)); val source = d.putLandOnBattlefield(p, converter.name)
        val forests = List(2) { d.putLandOnBattlefield(p, "Forest") }; victims(d, 3)
        val r = pay(d, scoped(d), "{U}{U}")
        r.error shouldBe null
        r.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
        forests.all { r.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
        r.state.getZone(ZoneKey(p, Zone.EXILE)).size shouldBe 2
        r.state.remainingManaObligations(p) shouldBe false
    }
    test("source text changes apply to graveyard selection filters") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = producer("Graveyard Changed", Costs.ExileFromGraveyard(1,
            GameObjectFilter.Creature.withSubtype(Subtype("Elf"))))
        val goblin = card("Graveyard Goblin") { typeLine = "Creature — Goblin"; power = 1; toughness = 1 }
        d.registerCards(listOf(sourceCard, goblin)); val source = d.putLandOnBattlefield(p, sourceCard.name)
        val victim = d.putCardInGraveyard(p, goblin.name)
        d.replaceState(d.state.updateEntity(source) { it.with(TextReplacementComponent(listOf(
            TextReplacement("Elf", "Goblin", TextReplacementCategory.CREATURE_TYPE)))) })
        val r = pay(d, scoped(d), "{U}")
        r.error shouldBe null; r.state.getZone(ZoneKey(p, Zone.EXILE)).contains(victim) shouldBe true
    }
    test("a large graveyard choice space is budgeted and failed search is atomic") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, variable.name); victims(d, 8); val before = scoped(d)
        val result = plan(d, before, "{U}{U}", 2) as ScopedManaPlanResult.Unknown
        result.reasons.contains(ScopedManaSearchLimit.NODE_BUDGET) shouldBe true
        before.getZone(ZoneKey(p, Zone.EXILE)) shouldBe emptyList()
    }
    test("explicit source selection limits production but permits unrelated graveyard cost objects") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, fixed.name); val other = d.putLandOnBattlefield(p, "Island")
        victims(d, 2)
        val r = pay(d, scoped(d), "{U}{U}", PaymentStrategy.Explicit(listOf(source)))
        r.error shouldBe null
        r.state.getEntity(other)!!.has<TappedComponent>() shouldBe false
        r.state.getZone(ZoneKey(p, Zone.EXILE)).size shouldBe 2
    }
    test("two exile atoms sharing one action selection stay explicitly unsupported") {
        val d = driver(); val p = d.activePlayer!!
        val ambiguous = producer("Graveyard Two Costs", Costs.Composite(Costs.ExileFromGraveyard(1),
            Costs.ExileFromGraveyard(1)))
        d.registerCards(listOf(ambiguous)); d.putLandOnBattlefield(p, ambiguous.name); victims(d, 2)
        val result = plan(d, scoped(d), "{U}") as ScopedManaPlanResult.Unknown
        result.reasons.contains(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION) shouldBe true
    }
    test("nested graveyard exile costs remain uncertain instead of silently skipping choices") {
        for (cost in listOf(Costs.ExileFromGraveyard(1), Costs.ExileXFromGraveyard())) {
            val d = driver(); val p = d.activePlayer!!
            val nested = producer("Graveyard Nested", Costs.Composite(Costs.Composite(cost)), DynamicAmount.XValue)
            d.registerCards(listOf(nested)); d.putLandOnBattlefield(p, nested.name); victims(d, 2)
            val result = plan(d, scoped(d), "{U}") as ScopedManaPlanResult.Unknown
            result.reasons.contains(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION) shouldBe true
        }
    }
    test("exile costs from hidden zones remain explicitly unsupported") {
        val d = driver(); val p = d.activePlayer!!
        val hidden = producer("Graveyard Hidden Cost", AbilityCost.Atom(CostAtom.ExileFrom(Zone.HAND)))
        d.registerCards(listOf(hidden)); d.putLandOnBattlefield(p, hidden.name)
        val result = plan(d, scoped(d), "{U}") as ScopedManaPlanResult.Unknown
        result.reasons.contains(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION) shouldBe true
    }
    test("forced paid play resumes after graveyard-cost production without consuming outer work") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, fixed.name); victims(d, 2); val spell = d.putCardInHand(p, paid.name)
        val ctx = EffectContext(sourceId = null, controllerId = p,
            pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(spell))))
        val before = scoped(d)
        val r = d.services.effectExecutorRegistry.execute(before, Effects.ForcePlay("chosen"), ctx)
        r.error shouldBe null
        val decision = r.pendingDecision as PlayCardDecision
        d.replaceState(r.state)
        val cast = d.submitDecision(p, PlayCardResponse(decision.id, CastSpell(p, spell)))
        cast.error shouldBe null
        cast.state.stack.contains(spell) shouldBe true
        cast.state.getZone(ZoneKey(p, Zone.EXILE)).size shouldBe 2
        cast.state.remainingManaObligations(p) shouldBe false
    }
})
