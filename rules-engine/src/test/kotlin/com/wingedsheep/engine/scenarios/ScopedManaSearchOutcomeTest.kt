package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor
import com.wingedsheep.engine.mechanics.mana.*
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.ManaSourceTag
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PlayerActionTiming
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScopedManaSearchOutcomeTest : FunSpec({
    val unsupported = card("Search Side Effect Producer") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.Composite(listOf(Effects.AddMana(Color.GREEN, 1), Effects.GainLife(1)))
            manaAbility = true
        }
    }
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + unsupported)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scoped(d: GameTestDriver, pool: ManaPoolComponent = ManaPoolComponent()): GameState {
        val player = d.activePlayer!!
        return d.state.updateEntity(player) { it.with(pool) }.pushContinuation(
            ManaSpendingObligationsContinuation(player,
                EffectContext(sourceId = null, controllerId = player), "search-outcome"))
    }
    fun search(d: GameTestDriver, state: GameState, budget: Int = 256,
        excluded: Set<com.wingedsheep.sdk.model.EntityId> = emptySet()) =
        ScopedManaActivationPlanner(d.services, budget).plan(state, d.activePlayer!!,
            ManaCost.parse("{G}"), context, excludeSources = excluded)

    test("a complete floating proof wins at the node budget") {
        val d = driver()
        val state = scoped(d, ManaPoolComponent(green = 1))
        (search(d, state, 1) is ScopedManaPlanResult.Found) shouldBe true
    }
    test("a complete floating proof needs no search budget") {
        val d = driver()
        (search(d, scoped(d, ManaPoolComponent(green = 1)), 0) is ScopedManaPlanResult.Found) shouldBe true
    }
    test("a hidden battlefield uniformly reports uncertainty for source proofs") {
        val d = driver(); val player = d.activePlayer!!
        val hidden = d.putLandOnBattlefield(player, "Mountain")
        d.putLandOnBattlefield(player, "Forest")
        d.replaceState(d.state.updateEntity(hidden) { it.with(FaceDownComponent) })
        val result = search(d, scoped(d)) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.HIDDEN_BATTLEFIELD)
        d.services.manaSolver.canPay(scoped(d), player, ManaCost.parse("{G}"), spellContext = context) shouldBe false
    }
    test("floating payment remains provable on a hidden battlefield") {
        val d = driver()
        val hidden = d.putLandOnBattlefield(d.activePlayer!!, "Mountain")
        d.replaceState(d.state.updateEntity(hidden) { it.with(FaceDownComponent) })
        (search(d, scoped(d, ManaPoolComponent(green = 1)), 0) is ScopedManaPlanResult.Found) shouldBe true
    }
    test("a production question outside supported answer shapes is unknown") {
        val d = driver(); val player = d.activePlayer!!
        val pausing = card("Search Optional Tap Bonus") {
            typeLine = "Enchantment"
            staticAbility {
                ability = AdditionalManaOnSourceTap(GameObjectFilter.Land, color = Color.BLUE,
                    rider = Effects.May(Effects.GainLife(1)))
            }
        }
        d.registerCards(listOf(pausing))
        d.putPermanentOnBattlefield(player, pausing.name)
        val forest = d.putLandOnBattlefield(player, "Forest")
        val state = scoped(d)
        val result = ScopedManaActivationPlanner(d.services).plan(state, player,
            ManaCost.parse("{G}{U}"), context) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.UNSUPPORTED_DECISION)
        state.pendingDecision shouldBe null
        state.getEntity(forest)!!.has<TappedComponent>() shouldBe false
    }
    test("an absent spending scope is outside the planner proof contract") {
        val d = driver()
        val result = search(d, d.state) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.NO_SPENDING_SCOPE)
    }
    test("a solver without an execution provider reports its proof boundary") {
        val d = driver()
        val solver = ManaSolver(d.services.cardRegistry, d.services.predicateEvaluator)
        val result = solver.planScopedActivations(scoped(d), d.activePlayer!!,
            ManaCost.parse("{G}"), context, 0, emptySet()) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.NO_EXECUTION_PROVIDER)
    }
    test("mana producing player actions are unsupported rather than impossible") {
        val d = driver(); val player = d.activePlayer!!
        val state = d.services.effectExecutorRegistry.execute(scoped(d),
            Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.AddMana(Color.GREEN, 1),
                PlayerActionTiming.ManaAbility, "Pay life for green mana"),
            EffectContext(sourceId = null, controllerId = player)).state
        val result = search(d, state) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION)
    }
    test("an unfinished production frame cannot become a floating payment proof") {
        val d = driver(); val player = d.activePlayer!!
        val source = d.putLandOnBattlefield(player, "Forest")
        val state = scoped(d, ManaPoolComponent(green = 1)).pushContinuation(
            ScopedManaProductionContinuation(player, source, "Forest",
                d.state.getEntity(source)!!.get<CardComponent>()!!,
                ManaPoolComponent(), ManaSourceTag(source), costsTap = true))
        val result = search(d, state) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.CONTINUATION_BOUNDARY)
        d.services.manaSolver.canPay(state, player, ManaCost.parse("{G}"), spellContext = context) shouldBe false
        val standalone = ManaSolver(d.services.cardRegistry, d.services.predicateEvaluator)
        for (solver in listOf(d.services.manaSolver, standalone)) {
            val processor = CastPaymentProcessor(d.services.zones, solver, d.services.costHandler,
                d.services.manaAbilitySideEffectExecutor)
            val payment = processor.processPayment(state, CastSpell(player, player),
                ManaCost.parse("{G}"), "Incomplete production", 0, context)
            payment.error!!.contains("could not determine") shouldBe true
            payment.state shouldBe state
            payment.events shouldBe emptyList()
        }
    }
    test("a completed activation wins when it consumes the last admitted node") {
        val d = driver(); val player = d.activePlayer!!
        val forest = d.putLandOnBattlefield(player, "Forest")
        val state = scoped(d)
        val result = search(d, state, 2) as ScopedManaPlanResult.Found
        result.execution.state.getEntity(forest)!!.has<TappedComponent>() shouldBe true
        state.getEntity(forest)!!.has<TappedComponent>() shouldBe false
    }
    test("no possible expansion is impossible even at the node budget") {
        val d = driver()
        search(d, scoped(d), 1) shouldBe ScopedManaPlanResult.Impossible
    }
    test("an omitted solvable expansion is unknown and cannot mutate the input") {
        val d = driver(); val player = d.activePlayer!!
        val forest = d.putLandOnBattlefield(player, "Forest")
        val state = scoped(d)
        val result = search(d, state, 1) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.NODE_BUDGET)
        state.getEntity(forest)!!.has<TappedComponent>() shouldBe false
        state.getEntity(player)!!.get<ManaPoolComponent>() shouldBe ManaPoolComponent()
    }
    test("an affordable unsupported activation prevents an impossibility claim") {
        val d = driver()
        d.putLandOnBattlefield(d.activePlayer!!, unsupported.name)
        val result = search(d, scoped(d)) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION)
    }
    test("an excluded unsupported source does not make the search unknown") {
        val d = driver()
        val source = d.putLandOnBattlefield(d.activePlayer!!, unsupported.name)
        search(d, scoped(d), excluded = setOf(source)) shouldBe ScopedManaPlanResult.Impossible
    }
    test("an unsupported source remains unknown when enumeration declines affordability") {
        val d = driver()
        val source = d.putLandOnBattlefield(d.activePlayer!!, unsupported.name)
        d.replaceState(d.state.updateEntity(source) { it.with(TappedComponent) })
        val result = search(d, scoped(d)) as ScopedManaPlanResult.Unknown
        result.reasons shouldBe setOf(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION)
    }
    test("a successful alternative dominates unsupported branches") {
        val d = driver(); val player = d.activePlayer!!
        val unsupportedSource = d.putLandOnBattlefield(player, unsupported.name)
        d.putLandOnBattlefield(player, "Forest")
        val result = search(d, scoped(d)) as ScopedManaPlanResult.Found
        result.execution.state.getEntity(unsupportedSource)!!.has<TappedComponent>() shouldBe false
        result.execution.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
    }
    test("unknown affordability stays false and payment reports uncertainty atomically") {
        val d = driver(); val player = d.activePlayer!!
        val forest = d.putLandOnBattlefield(player, "Forest")
        val state = scoped(d)
        val solver = ManaSolver(d.services.cardRegistry, d.services.predicateEvaluator) {
            ScopedManaActivationPlanner(d.services, 1)
        }
        solver.canPay(state, player, ManaCost.parse("{G}"), spellContext = context) shouldBe false
        val processor = CastPaymentProcessor(d.services.zones, solver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor)
        val result = processor.processPayment(state, CastSpell(player, player),
            ManaCost.parse("{G}"), "Search outcome", 0, context)
        result.error!!.contains("could not determine", ignoreCase = true) shouldBe true
        result.state shouldBe state
        result.events shouldBe emptyList()
        state.getEntity(forest)!!.has<TappedComponent>() shouldBe false
    }
})
