package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the per-counter template — `CountersPlacedEvent(perCounter = true)`,
 * "whenever **a** -1/-1 counter is put on a creature" (Flourishing Defenses). Unlike the
 * per-recipient template ("one or more counters"), it triggers once for each counter placed.
 */
class CountersPlacedPerCounterTriggerTest : FunSpec({

    val perCounterObserver = card("Per Counter Observer") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature)
                .getsCounters(CounterType.MINUS_ONE_MINUS_ONE, perCounter = true)
            effect = Effects.DrawCards(1)
        }
    }

    val bear = card("Per Counter Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(perCounterObserver, bear))
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

    fun placed(entityId: EntityId, type: CounterType, amount: Int) =
        CountersAddedEvent(entityId, type, amount, "", placedBy = null)

    test("two counters in one placement trigger twice, each with a count of one") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Per Counter Observer")
        val bearId = driver.putCreatureOnBattlefield(driver.player2, "Per Counter Test Bear")

        val triggers = triggersOf(driver, listOf(placed(bearId, CounterType.MINUS_ONE_MINUS_ONE, 2)), observer)

        triggers shouldHaveSize 2
        triggers.forEach {
            it.triggerContext.triggeringEntityId shouldBe bearId
            it.triggerContext.counterCount shouldBe 1
        }
    }

    test("counters across recipients and events add up one trigger per counter") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Per Counter Observer")
        val bearA = driver.putCreatureOnBattlefield(driver.player1, "Per Counter Test Bear")
        val bearB = driver.putCreatureOnBattlefield(driver.player2, "Per Counter Test Bear")

        val triggers = triggersOf(
            driver,
            listOf(
                placed(bearA, CounterType.MINUS_ONE_MINUS_ONE, 1),
                placed(bearB, CounterType.MINUS_ONE_MINUS_ONE, 3),
                placed(bearA, CounterType.MINUS_ONE_MINUS_ONE, 1),
            ),
            observer
        )

        triggers shouldHaveSize 5
        triggers.count { it.triggerContext.triggeringEntityId == bearA } shouldBe 2
        triggers.count { it.triggerContext.triggeringEntityId == bearB } shouldBe 3
    }

    test("counters of another kind don't trigger it") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Per Counter Observer")
        val bearId = driver.putCreatureOnBattlefield(driver.player1, "Per Counter Test Bear")

        triggersOf(driver, listOf(placed(bearId, CounterType.PLUS_ONE_PLUS_ONE, 2)), observer) shouldHaveSize 0
    }

    test("a mixed placement on one creature fans out only the matching kind") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Per Counter Observer")
        val bearId = driver.putCreatureOnBattlefield(driver.player1, "Per Counter Test Bear")

        val triggers = triggersOf(
            driver,
            listOf(
                placed(bearId, CounterType.PLUS_ONE_PLUS_ONE, 1),
                placed(bearId, CounterType.MINUS_ONE_MINUS_ONE, 1),
            ),
            observer
        )

        triggers shouldHaveSize 1
    }

    test("per-counter is exclusive with the at-most-once multiplicities") {
        shouldThrow<IllegalArgumentException> {
            EventPattern.CountersPlacedEvent(CounterType.MINUS_ONE_MINUS_ONE, batch = true, perCounter = true)
        }
        shouldThrow<IllegalArgumentException> {
            EventPattern.CountersPlacedEvent(CounterType.MINUS_ONE_MINUS_ONE, firstTimeEachTurn = true, perCounter = true)
        }
    }
})
