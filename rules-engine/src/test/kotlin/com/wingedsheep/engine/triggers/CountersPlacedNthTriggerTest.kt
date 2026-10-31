package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the ordinal template — `CountersPlacedEvent(nth = N)`, "when the **twelfth**
 * hour counter is put on this artifact" (Midnight Clock). It fires for the placement that takes the
 * count of that kind from below N to N or more, and for no other.
 */
class CountersPlacedNthTriggerTest : FunSpec({

    val clock = card("Nth Counter Clock") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.self.getsCounters(CounterType.HOUR, nth = 3)
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(clock))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        return driver
    }

    fun triggersOf(driver: GameTestDriver, events: List<CountersAddedEvent>, sourceId: EntityId) =
        TriggerDetector(
            driver.cardRegistry,
            predicateEvaluator = PredicateEvaluator(cardRegistry = null),
            conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions
        )
            .detectTriggers(driver.state, events)
            .filter { it.ability.trigger is EventPattern.CountersPlacedEvent && it.sourceId == sourceId }

    fun placed(entityId: EntityId, amount: Int, totalCount: Int?, type: CounterType = CounterType.HOUR) =
        CountersAddedEvent(entityId, type, amount, "", placedBy = null, totalCount = totalCount)

    test("the placement reaching N fires it; the ones before and after don't") {
        val driver = createDriver()
        val clockId = driver.putPermanentOnBattlefield(driver.player1, "Nth Counter Clock")

        triggersOf(driver, listOf(placed(clockId, 1, totalCount = 2)), clockId) shouldHaveSize 0
        triggersOf(driver, listOf(placed(clockId, 1, totalCount = 3)), clockId) shouldHaveSize 1
        triggersOf(driver, listOf(placed(clockId, 1, totalCount = 4)), clockId) shouldHaveSize 0
    }

    test("a single placement jumping past N fires it once") {
        val driver = createDriver()
        val clockId = driver.putPermanentOnBattlefield(driver.player1, "Nth Counter Clock")

        triggersOf(driver, listOf(placed(clockId, 3, totalCount = 5)), clockId) shouldHaveSize 1
    }

    test("several placements in one batch: only the one that crossed N counts") {
        val driver = createDriver()
        val clockId = driver.putPermanentOnBattlefield(driver.player1, "Nth Counter Clock")

        val triggers = triggersOf(
            driver,
            listOf(placed(clockId, 1, totalCount = 2), placed(clockId, 1, totalCount = 3), placed(clockId, 1, totalCount = 4)),
            clockId
        )
        triggers shouldHaveSize 1
        // A batch where no placement crosses N fires nothing, merged or not.
        triggersOf(
            driver,
            listOf(placed(clockId, 1, totalCount = 4), placed(clockId, 1, totalCount = 5)),
            clockId
        ) shouldHaveSize 0
    }

    test("another counter kind doesn't count toward N") {
        val driver = createDriver()
        val clockId = driver.putPermanentOnBattlefield(driver.player1, "Nth Counter Clock")

        triggersOf(driver, listOf(placed(clockId, 3, totalCount = 3, type = CounterType.CHARGE)), clockId) shouldHaveSize 0
    }

    test("without a recorded total it reads the live count") {
        val driver = createDriver()
        val clockId = driver.putPermanentOnBattlefield(driver.player1, "Nth Counter Clock")
        driver.replaceState(
            driver.state.updateEntity(clockId) { it.with(CountersComponent().withAdded(CounterType.HOUR, 3)) }
        )

        triggersOf(driver, listOf(placed(clockId, 1, totalCount = null)), clockId) shouldHaveSize 1
        triggersOf(driver, listOf(placed(clockId, 0, totalCount = null)), clockId) shouldHaveSize 0
    }

    test("description and invariants") {
        EventPattern.CountersPlacedEvent(CounterType.HOUR, nth = 12).description shouldBe
            "the twelfth hour counter is put on a card or permanent"
        EventPattern.CountersPlacedEvent(CounterType.HOUR, nth = 21).description shouldBe
            "the 21st hour counter is put on a card or permanent"
        shouldThrow<IllegalArgumentException> { EventPattern.CountersPlacedEvent(null, nth = 12) }
        shouldThrow<IllegalArgumentException> { EventPattern.CountersPlacedEvent(CounterType.HOUR, nth = 0) }
        shouldThrow<IllegalArgumentException> { EventPattern.CountersPlacedEvent(CounterType.HOUR, perCounter = true, nth = 2) }
        shouldThrow<IllegalArgumentException> { EventPattern.CountersPlacedEvent(CounterType.HOUR, batch = true, nth = 2) }
    }
})
