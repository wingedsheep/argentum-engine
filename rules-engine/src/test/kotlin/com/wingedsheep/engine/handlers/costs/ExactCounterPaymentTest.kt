package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ExactCounterPaymentTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.counters(player: EntityId, type: CounterType = CounterType.ENERGY) =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(type) ?: 0
    fun GameTestDriver.give(player: EntityId, n: Int, type: CounterType = CounterType.ENERGY) =
        replaceState(state.updateEntity(player) { it.with(CountersComponent(mapOf(type to n))) })
    fun GameTestDriver.run(effect: Effect, context: EffectContext = EffectContext(sourceId = null, controllerId = player1)) =
        services.effectExecutorRegistry.execute(state, effect, context).also { replaceState(it.state) }

    test("target-derived payment spends exactly that amount and emits one removal event") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.give(d.player1, 5)
        val result = d.run(Effects.PayExactCounters(CounterType.ENERGY, DynamicAmounts.targetManaValue()),
            EffectContext(sourceId = null, controllerId = d.player1, targets = listOf(ChosenTarget.Permanent(bear))))
        result.error shouldBe null
        d.counters(d.player1) shouldBe 3
        result.events.filterIsInstance<CountersRemovedEvent>().single().amount shouldBe 2
    }
    test("cannot partially pay or borrow an opponent's counters") {
        val d = driver()
        d.give(d.player1, 2)
        d.give(d.player2, 10)
        val before = d.state
        val result = d.run(Effects.PayExactCounters(CounterType.ENERGY, 3))
        result.error shouldNotBe null
        result.state shouldBe before
        result.events shouldBe emptyList()
    }
    test("zero and negative computed amounts succeed without counters or removal events") {
        for (n in listOf(0, -3)) {
            val d = driver()
            val before = d.state
            val result = d.run(Effects.PayExactCounters(CounterType.ENERGY, DynamicAmount.Fixed(n)))
            result.error shouldBe null
            result.state shouldBe before
            result.events shouldBe emptyList()
        }
    }
    test("MayPay checks the payment's payer and retains the original controller across the decision") {
        val d = driver()
        d.give(d.player2, 4, CounterType.POISON)
        val effect = Effects.MayPay(
            Effects.PayExactCounters(CounterType.POISON, DynamicAmount.XValue, Player.AnOpponent),
            Effects.GainLife(DynamicAmount.XValue), Effects.LoseLife(1, EffectTarget.Controller),
            decisionMaker = EffectTarget.PlayerRef(Player.AnOpponent))
        d.run(effect, EffectContext(sourceId = null, controllerId = d.player1, xValue = 3)).error shouldBe null
        (d.state.pendingDecision as YesNoDecision).playerId shouldBe d.player2
        d.submitYesNo(d.player2, true).error shouldBe null
        d.counters(d.player2, CounterType.POISON) shouldBe 1
        d.getLifeTotal(d.player1) shouldBe 23
        d.getLifeTotal(d.player2) shouldBe 20
    }
    test("unaffordable and declined payments take otherwise without spending") {
        for (available in listOf(1, 4)) {
            val d = driver()
            d.give(d.player1, available)
            d.run(Effects.MayPay(Effects.PayExactCounters(CounterType.ENERGY, 3),
                Effects.GainLife(5), Effects.LoseLife(2, EffectTarget.Controller))).error shouldBe null
            if (available >= 3) d.submitYesNo(d.player1, false).error shouldBe null
            d.state.pendingDecision shouldBe null
            d.counters(d.player1) shouldBe available
            d.getLifeTotal(d.player1) shouldBe 18
        }
    }
    test("zero payment is optional and its benefit requires acknowledgment") {
        for (accept in listOf(false, true)) {
            val d = driver()
            d.run(Effects.MayPay(Effects.PayExactCounters(CounterType.ENERGY, 0), Effects.GainLife(2)))
            d.state.pendingDecision shouldNotBe null
            d.submitYesNo(d.player1, accept).error shouldBe null
            d.getLifeTotal(d.player1) shouldBe if (accept) 22 else 20
        }
    }
    test("pipeline variable amount survives the payment pause") {
        val d = driver()
        d.give(d.player1, 7)
        val context = EffectContext(sourceId = null, controllerId = d.player1,
            pipeline = PipelineState(storedNumbers = mapOf("price" to 4)))
        d.run(Effects.MayPay(Effects.PayExactCounters(CounterType.ENERGY, DynamicAmount.VariableReference("price")),
            Effects.GainLife(DynamicAmount.VariableReference("price"))), context)
        d.submitYesNo(d.player1, true).error shouldBe null
        d.counters(d.player1) shouldBe 3
        d.getLifeTotal(d.player1) shouldBe 24
    }
    test("dynamic reflexive payment suppresses an unaffordable prompt") {
        val d = driver()
        d.give(d.player1, 1)
        d.run(Effects.ReflexiveTrigger(
            action = Effects.PayExactCounters(CounterType.ENERGY, DynamicAmount.XValue),
            reflexiveEffect = Effects.GainLife(3)),
            EffectContext(sourceId = null, controllerId = d.player1, xValue = 2)).error shouldBe null
        d.state.pendingDecision shouldBe null
        d.counters(d.player1) shouldBe 1
        d.getLifeTotal(d.player1) shouldBe 20
    }
    test("dynamic prices read projected power rather than printed power") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.give(d.player1, 7)
        val context = EffectContext(sourceId = bear, controllerId = d.player1,
            targets = listOf(ChosenTarget.Permanent(bear)))
        d.run(Effects.ModifyStats(3, 0, EffectTarget.ContextTarget(0)), context).error shouldBe null
        d.run(Effects.PayExactCounters(CounterType.ENERGY, DynamicAmounts.targetPower()), context).error shouldBe null
        d.counters(d.player1) shouldBe 2
    }
    test("successful dynamic reflexive payment emits the reflexive event after removal") {
        val d = driver()
        d.give(d.player1, 5)
        val result = d.run(Effects.ReflexiveTrigger(
            action = Effects.PayExactCounters(CounterType.ENERGY, DynamicAmount.XValue),
            reflexiveEffect = Effects.GainLife(3), optional = false),
            EffectContext(sourceId = null, controllerId = d.player1, xValue = 2))
        result.error shouldBe null
        d.counters(d.player1) shouldBe 3
        result.events.filterIsInstance<CountersRemovedEvent>().single().amount shouldBe 2
        result.events.filterIsInstance<com.wingedsheep.engine.core.ReflexiveAbilityTriggeredEvent>().size shouldBe 1
        d.getLifeTotal(d.player1) shouldBe 20
    }

})
