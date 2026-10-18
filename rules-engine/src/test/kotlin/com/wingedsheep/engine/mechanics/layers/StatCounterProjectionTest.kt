package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Stat counters in layer 7c (CR 122.1a — a +X/+Y counter adds X to power and Y to toughness;
 * CR 613.4c places counters in the P/T layer). `applyCounters` sums each kind's
 * `CounterType.ptDelta`, so every printed size works without an engine branch of its own.
 */
class StatCounterProjectionTest : FunSpec({
    val owner = EntityId.of("owner")
    val id = EntityId.of("creature")
    val definition = card("Counter Witness") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 4
        toughness = 5
    }

    fun projectWith(vararg counters: Pair<CounterType, Int>): Pair<Int?, Int?> {
        val state = GameState(
            entities = mapOf(id to CardEntityFactory.create(definition, owner).with(CountersComponent(mapOf(*counters)))),
            zones = mapOf(ZoneKey(owner, Zone.BATTLEFIELD) to listOf(id)),
        )
        val projected = StateProjector().project(state)
        return projected.getPower(id) to projected.getToughness(id)
    }

    test("a -0/-2 counter lowers only toughness, by two each") {
        projectWith(CounterType.MINUS_ZERO_MINUS_TWO to 2) shouldBe (4 to 1)
    }

    test("mixed kinds sum independently") {
        projectWith(
            CounterType.PLUS_ONE_PLUS_ONE to 1,
            CounterType.MINUS_ZERO_MINUS_TWO to 1,
            CounterType.PLUS_TWO_PLUS_ZERO to 1,
            CounterType.MINUS_ONE_MINUS_ZERO to 3,
        ) shouldBe (4 to 4)
    }

    test("any stat-shaped kind sums, even one no card names yet") {
        projectWith(CounterType.of("+3/-1") to 1) shouldBe (7 to 4)
    }

    test("non-stat counters leave power and toughness alone") {
        projectWith(CounterType.CHARGE to 3, CounterType.FIRST_STRIKE to 1) shouldBe (4 to 5)
    }
})
