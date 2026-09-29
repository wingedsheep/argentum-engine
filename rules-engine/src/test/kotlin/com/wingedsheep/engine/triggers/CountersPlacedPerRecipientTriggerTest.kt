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
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the per-recipient counter-placement trigger — the non-batch
 * `EventPattern.CountersPlacedEvent` — and its "on a permanent **or player**" widening
 * (`includePlayers`, All Will Be One).
 *
 * CR 603.2c: an ability triggers once each time its trigger event occurs. The engine emits one
 * [CountersAddedEvent] per counter kind (proliferate, CR 701.34a) and per toxic source
 * (CR 702.164c), but one simultaneous placement is one event for its recipient, so the detector folds
 * a detection pass's placements on the same recipient into one firing with the summed count.
 */
class CountersPlacedPerRecipientTriggerTest : FunSpec({

    // "Whenever you put one or more counters on a permanent or player …" (All Will Be One).
    val orPlayerObserver = card("Permanent Or Player Counter Observer") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Permanent).getsCounters(by = Player.You, orPlayer = true)
            effect = Effects.DrawCards(1)
        }
    }

    // The same with no subject filter and no player widening: players must still be excluded.
    val anyObjectObserver = card("Any Object Counter Observer") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a().getsCounters()
            effect = Effects.DrawCards(1)
        }
    }

    val bear = card("Per Recipient Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(orPlayerObserver, anyObjectObserver, bear))
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

    fun placed(entityId: EntityId, type: CounterType, amount: Int, placedBy: EntityId?) =
        CountersAddedEvent(entityId, type, amount, "", placedBy = placedBy)

    test("a player receiving counters fires the permanent-or-player template") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Permanent Or Player Counter Observer")

        val triggers = triggersOf(driver, listOf(placed(driver.player2, CounterType.POISON, 1, driver.player1)), observer)

        triggers shouldHaveSize 1
        triggers.first().triggerContext.triggeringEntityId shouldBe driver.player2
        triggers.first().triggerContext.counterCount shouldBe 1
    }

    test("without the player widening a player is never a recipient, even for an unfiltered subject") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Any Object Counter Observer")

        triggersOf(driver, listOf(placed(driver.player2, CounterType.POISON, 1, driver.player1)), observer) shouldHaveSize 0
    }

    test("the placer axis still applies to a player recipient") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Permanent Or Player Counter Observer")

        withClue("an opponent's placement") {
            triggersOf(driver, listOf(placed(driver.player2, CounterType.POISON, 1, driver.player2)), observer) shouldHaveSize 0
        }
        withClue("an unattributed placement never matches a placer-restricted trigger") {
            triggersOf(driver, listOf(placed(driver.player2, CounterType.POISON, 1, null)), observer) shouldHaveSize 0
        }
    }

    test("several kinds on one permanent in one pass fire once with the summed count") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Permanent Or Player Counter Observer")
        val bearId = driver.putCreatureOnBattlefield(driver.player1, "Per Recipient Test Bear")

        val triggers = triggersOf(
            driver,
            listOf(
                placed(bearId, CounterType.PLUS_ONE_PLUS_ONE, 1, driver.player1),
                placed(bearId, CounterType.OIL, 1, driver.player1),
            ),
            observer
        )

        triggers shouldHaveSize 1
        triggers.first().triggerContext.counterCount shouldBe 2
    }

    test("simultaneous poison from two sources on one player fires once; distinct recipients fire separately") {
        val driver = createDriver()
        val observer = driver.putPermanentOnBattlefield(driver.player1, "Permanent Or Player Counter Observer")
        val bearId = driver.putCreatureOnBattlefield(driver.player1, "Per Recipient Test Bear")

        val triggers = triggersOf(
            driver,
            listOf(
                placed(driver.player2, CounterType.POISON, 1, driver.player1),
                placed(bearId, CounterType.PLUS_ONE_PLUS_ONE, 3, driver.player1),
                placed(driver.player2, CounterType.POISON, 2, driver.player1),
            ),
            observer
        )

        triggers shouldHaveSize 2
        triggers.associate { it.triggerContext.triggeringEntityId to it.triggerContext.counterCount } shouldBe
            mapOf(driver.player2 to 3, bearId to 3)
    }
})
