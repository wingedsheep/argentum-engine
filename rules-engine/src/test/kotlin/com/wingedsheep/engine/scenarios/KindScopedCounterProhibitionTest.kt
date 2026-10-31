package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CantReceiveCounters(filter, counterType)` — a prohibition scoped to one kind of counter
 * ("Creatures your opponents control can't have +1/+1 counters put on them", Blightbeetle).
 *
 * Only that kind is stopped, only on the matching permanents; other kinds still go on, counters
 * already there stay, a creature that would enter with that kind enters without it, and a cost
 * that would put that kind on a matching permanent can't be paid (CR 614.17b — the event can't
 * happen, so the cost can't be chosen).
 */
class KindScopedCounterProhibitionTest : FunSpec({
    val plus = CounterType.PLUS_ONE_PLUS_ONE
    val minus = CounterType.MINUS_ONE_MINUS_ONE

    val prohibitor = card("Test Plus Counter Prohibitor") {
        typeLine = "Creature — Insect"; power = 1; toughness = 1
        staticAbility { ability = CantReceiveCounters(GroupFilter.AllCreaturesOpponentsControl, plus) }
    }
    val plusCoster = card("Test Plus Coster") {
        typeLine = "Creature — Elf"; power = 1; toughness = 1
        activatedAbility {
            cost = Costs.PutCounterOnSelf(plus)
            effect = Effects.GainLife(1)
        }
    }
    val minusCoster = card("Test Minus Coster") {
        typeLine = "Creature — Elf"; power = 3; toughness = 3
        activatedAbility {
            cost = Costs.PutCounterOnSelf(minus)
            effect = Effects.GainLife(1)
        }
    }

    // player2 controls the prohibitor, so player1's creatures are the ones it covers.
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(prohibitor, plusCoster, minusCoster))
        initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
        putCreatureOnBattlefield(player2, prohibitor.name)
    }
    fun GameTestDriver.run(effect: Effect, controller: EntityId, collection: List<EntityId> = emptyList()) =
        services.effectExecutorRegistry.execute(state, effect, EffectContext(sourceId = null, controllerId = controller,
            pipeline = PipelineState(storedCollections = mapOf("cards" to collection))))
    fun com.wingedsheep.engine.state.GameState.count(id: EntityId, type: CounterType) =
        getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("the prohibited kind isn't put on a covered creature; nothing is announced") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val result = d.run(Effects.AddCounters(plus, 2, EffectTarget.SpecificEntity(bears)), d.player1)
        result.state.count(bears, plus) shouldBe 0
        result.events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList()
    }

    test("other kinds still go on a covered creature") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val result = d.run(Effects.AddCounters(minus, 1, EffectTarget.SpecificEntity(bears)), d.player2)
        result.state.count(bears, minus) shouldBe 1
        result.events.filterIsInstance<CountersAddedEvent>().single().counterType shouldBe minus
    }

    test("creatures outside the filter still get the prohibited kind") {
        val d = driver()
        val own = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val result = d.run(Effects.AddCounters(plus, 1, EffectTarget.SpecificEntity(own)), d.player2)
        result.state.count(own, plus) shouldBe 1
    }

    test("counters already on a covered creature stay — it is not a state-based removal") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(prohibitor))
            initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
        }
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.addComponent(bears, CountersComponent(mapOf(plus to 2)))
        d.putCreatureOnBattlefield(d.player2, prohibitor.name)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.count(bears, plus) shouldBe 2
        d.state.projectedState.getPower(bears) shouldBe 4
    }

    test("a covered creature that would enter with the prohibited kind enters without it") {
        val d = driver()
        val bears = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val result = d.run(Effects.Move(EffectTarget.SpecificEntity(bears), Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.PlayerRef(Player.You), addCounterType = plus), d.player1)
        (bears in result.state.getBattlefield()) shouldBe true
        result.state.count(bears, plus) shouldBe 0
        result.events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList()
    }

    test("a group placement skips only the covered creature and announces only what was placed") {
        val d = driver()
        val theirs = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val mine = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val result = d.run(Effects.AddCountersToCollection("cards", plus), d.player2, listOf(theirs, mine))
        result.state.count(theirs, plus) shouldBe 0
        result.state.count(mine, plus) shouldBe 1
        result.events.filterIsInstance<CountersAddedEvent>().map { it.entityId } shouldBe listOf(mine)
    }

    test("moving the prohibited kind onto a covered creature leaves it on the source (CR 122.5)") {
        val d = driver()
        val from = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.addComponent(from, CountersComponent(mapOf(plus to 2, minus to 1)))
        val onto = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        val one = d.run(Effects.MoveCounters(plus, com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(2),
            EffectTarget.SpecificEntity(from), EffectTarget.SpecificEntity(onto)), d.player2)
        one.state.count(from, plus) shouldBe 2
        one.state.count(onto, plus) shouldBe 0
        one.events shouldBe emptyList()

        // Moving every counter moves only the kinds the destination can have.
        val all = d.run(Effects.MoveAllCounters(EffectTarget.SpecificEntity(from), EffectTarget.SpecificEntity(onto)), d.player2)
        all.state.count(from, plus) shouldBe 2
        all.state.count(from, minus) shouldBe 0
        all.state.count(onto, minus) shouldBe 1
        all.state.count(onto, plus) shouldBe 0
    }

    test("a cost that puts the prohibited kind on a covered permanent can't be paid; other kinds can") {
        val d = driver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val plusSource = d.putCreatureOnBattlefield(d.player1, plusCoster.name)
        val minusSource = d.putCreatureOnBattlefield(d.player1, minusCoster.name)
        fun offered(source: EntityId) =
            d.legalActions(d.player1).any { (it.action as? ActivateAbility)?.sourceId == source }
        offered(plusSource) shouldBe false
        offered(minusSource) shouldBe true
    }
})
