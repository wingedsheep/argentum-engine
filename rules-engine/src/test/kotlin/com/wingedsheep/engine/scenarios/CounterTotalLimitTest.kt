package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.permanent.counters.AddCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.AddCountersWithLimitExecutor
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersWithLimitEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CounterTotalLimitTest : FunSpec({
    val kind = CounterType.PLUS_ONE_PLUS_ZERO
    val doubler = card("Test Counter Doubler") {
        typeLine = "Enchantment"
        replacementEffect(DoubleCounterPlacement(appliesTo = EventPattern.CounterPlacementEvent(kind, Recipient.AnyPermanent)))
    }
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + doubler)
        it.initMirrorMatch(Deck.of("Mountain" to 40))
    }
    fun effect(count: Int, limit: Int, target: EffectTarget = EffectTarget.Self) =
        AddCountersWithLimitEffect(kind, DynamicAmount.Fixed(count), DynamicAmount.Fixed(limit), target)

    test("replacement doubling cannot exceed the resulting total limit and event records actual placement") {
        val d = driver()
        val recipient = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.addComponent(recipient, CountersComponent(mapOf(kind to 5)))
        d.putPermanentOnBattlefield(d.player1, doubler.name)
        val original = d.state
        val result = AddCountersWithLimitExecutor(d.services.dynamicAmountEvaluator).execute(
            original, effect(2, 7), EffectContext(sourceId = recipient, controllerId = d.player1)
        )
        result.state.getEntity(recipient)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 7
        original.getEntity(recipient)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 5
        val event = result.events.single() as CountersAddedEvent
        event.amount shouldBe 2
        event.placedBy shouldBe d.player1
        event.firstThisTurn shouldBe true
    }
    test("the limit does not affect other placements or remove excess counters") {
        val d = driver()
        val recipient = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.addComponent(recipient, CountersComponent(mapOf(kind to 7)))
        val context = EffectContext(sourceId = recipient, controllerId = d.player1)
        val ordinary = AddCountersExecutor(d.services.predicateEvaluator).execute(d.state, AddCountersEffect(kind, 2, EffectTarget.Self), context)
        ordinary.state.getEntity(recipient)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 9
        val limited = AddCountersWithLimitExecutor(d.services.dynamicAmountEvaluator).execute(ordinary.state, effect(3, 7), context)
        limited.state shouldBe ordinary.state
        limited.events shouldBe emptyList()
    }
    test("zero amount does not become positive through a replacement") {
        val d = driver()
        val recipient = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.putPermanentOnBattlefield(d.player1, doubler.name)
        val result = AddCountersWithLimitExecutor(d.services.dynamicAmountEvaluator).execute(d.state, effect(0, 7), EffectContext(sourceId = recipient, controllerId = d.player1))
        result.state shouldBe d.state
        result.events shouldBe emptyList()
    }
    test("player recipients and other counter types use the same bounded placement") {
        val d = driver()
        val bounded = AddCountersWithLimitEffect(CounterType.ENERGY, DynamicAmount.Fixed(8), DynamicAmount.Fixed(3), EffectTarget.PlayerRef(Player.You))
        val result = AddCountersWithLimitExecutor(d.services.dynamicAmountEvaluator).execute(d.state, bounded, EffectContext(sourceId = null, controllerId = d.player1))
        result.state.getEntity(d.player1)!!.get<CountersComponent>()!!.getCount(CounterType.ENERGY) shouldBe 3
        (result.events.single() as CountersAddedEvent).amount shouldBe 3
    }
    test("missing recipient is a no-op") {
        val d = driver()
        val result = AddCountersWithLimitExecutor(d.services.dynamicAmountEvaluator).execute(d.state, effect(1, 7), EffectContext(sourceId = null, controllerId = d.player1))
        result.state shouldBe d.state
        result.events shouldBe emptyList()
    }

    test("dynamic number ceiling reads the original X before replacing it") {
        val d = driver()
        val executor = com.wingedsheep.engine.handlers.effects.composite.ChooseNumberThenExecutor(
            com.wingedsheep.engine.handlers.DecisionHandler(), d.services.dynamicAmountEvaluator
        )
        val choice = com.wingedsheep.sdk.scripting.effects.ChooseNumberThenEffect(
            then = effect(1, 7), maxValue = DynamicAmount.XValue
        )
        val result = executor.execute(d.state, choice, EffectContext(sourceId = null, controllerId = d.player1, xValue = 12))
        (result.state.pendingDecision as com.wingedsheep.engine.core.ChooseNumberDecision).maxValue shouldBe 12
    }
    test("an empty dynamic number range skips the inner instruction") {
        val d = driver()
        val executor = com.wingedsheep.engine.handlers.effects.composite.ChooseNumberThenExecutor(
            com.wingedsheep.engine.handlers.DecisionHandler(), d.services.dynamicAmountEvaluator
        )
        val choice = com.wingedsheep.sdk.scripting.effects.ChooseNumberThenEffect(
            then = effect(1, 7), minValue = 1, maxValue = DynamicAmount.Fixed(0)
        )
        val result = executor.execute(d.state, choice, EffectContext(sourceId = null, controllerId = d.player1))
        result.state shouldBe d.state
        result.events shouldBe emptyList()
    }

    test("a projected prohibition prevents bounded placement") {
        val d = driver()
        val warded = card("Test Bounded Warded Creature") {
            typeLine = "Creature"
            power = 1
            toughness = 1
            staticAbility { ability = com.wingedsheep.sdk.scripting.CantReceiveCounters(com.wingedsheep.sdk.scripting.filters.unified.GroupFilter.source()) }
        }
        d.registerCards(listOf(warded))
        val recipient = d.putCreatureOnBattlefield(d.player1, warded.name)
        val result = AddCountersWithLimitExecutor(d.services.dynamicAmountEvaluator).execute(d.state, effect(3, 7), EffectContext(sourceId = recipient, controllerId = d.player1))
        result.state shouldBe d.state
        result.events shouldBe emptyList()
    }
})
