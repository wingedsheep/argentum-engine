package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.AffectsFilter
import com.wingedsheep.engine.mechanics.layers.AffectsFilterResolver
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.HasCounter(type, minCount)` — the threshold form of the counter filter, "with
 * three or more +1/+1 counters on them" (Runadi, Behemoth Caller). Checked on both readers that
 * answer it for battlefield permanents: the [PredicateEvaluator] (targets, counts) and the
 * [AffectsFilterResolver] (the layer system's group statics).
 */
class CounterThresholdPredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val resolver = AffectsFilterResolver(evaluator)
    val player = EntityId.generate()

    fun creature(vararg counters: Pair<CounterType, Int>) = ComponentContainer()
        .with(CardComponent(
            cardDefinitionId = "Test Creature",
            name = "Test Creature",
            manaCost = ManaCost(emptyList()),
            typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
            ownerId = player,
            baseStats = CreatureStats(2, 2)
        ))
        .with(OwnerComponent(player))
        .with(ControllerComponent(player))
        .let { if (counters.isEmpty()) it else it.with(CountersComponent(counters.toMap())) }

    val none = EntityId.generate()
    val two = EntityId.generate()
    val three = EntityId.generate()
    val five = EntityId.generate()
    val threeOther = EntityId.generate()
    val state = listOf(
        none to creature(),
        two to creature(CounterType.PLUS_ONE_PLUS_ONE to 2),
        three to creature(CounterType.PLUS_ONE_PLUS_ONE to 3),
        five to creature(CounterType.PLUS_ONE_PLUS_ONE to 5),
        threeOther to creature(CounterType.TIME to 3),
    ).fold(GameState().withEntity(player, ComponentContainer())) { s, (id, c) ->
        s.withEntity(id, c).addToZone(ZoneKey(player, Zone.BATTLEFIELD), id)
    }

    val threshold = GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE, atLeast = 3)

    test("the evaluator matches only creatures with at least the threshold of that kind") {
        val ctx = PredicateContext(controllerId = player)
        listOf(none, two, three, five, threeOther)
            .filter { evaluator.matches(state, state.projectedState, it, threshold, ctx) }
            .shouldContainExactlyInAnyOrder(three, five)
    }

    test("the layer resolver agrees") {
        resolver.resolveAffectedEntities(state, three, AffectsFilter.Generic(GroupFilter(threshold)))
            .shouldContainExactlyInAnyOrder(three, five)
    }

    test("the default is still 'at least one'") {
        val ctx = PredicateContext(controllerId = player)
        val any = GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE)
        listOf(none, two, three, five, threeOther)
            .filter { evaluator.matches(state, state.projectedState, it, any, ctx) }
            .shouldContainExactlyInAnyOrder(two, three, five)
    }

    test("a threshold below one is rejected, and the description reads the count") {
        shouldThrow<IllegalArgumentException> { StatePredicate.HasCounter(CounterType.PLUS_ONE_PLUS_ONE, 0) }
        StatePredicate.HasCounter(CounterType.PLUS_ONE_PLUS_ONE, 3).description shouldBe
            "with 3 or more +1/+1 counters"
    }
})
