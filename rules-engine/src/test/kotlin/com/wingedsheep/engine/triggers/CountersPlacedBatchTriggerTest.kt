package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the **batched** counter-placement trigger —
 * `EventPattern.CountersPlacedEvent(batch = true)`, detected by
 * `TriggerDetector.detectCountersPlacedBatchTriggers`.
 *
 * CR 603.2c: "An ability triggers only once each time its trigger event occurs. However, it can
 * trigger repeatedly if one event contains multiple occurrences." A single effect that puts counters
 * on several permanents is one occurrence for the "one or more counters on **one or more**
 * permanents" template (Invisible Woman, Sue Storm) and several occurrences for the per-permanent
 * "on **a** permanent" template (Stalwart Successor). Both observers are built here from the same
 * pattern so every case asserts the two multiplicities side by side — the batch flag is the only
 * difference between them.
 *
 * The detector is driven directly with synthesized [CountersAddedEvent]s, which is exactly the shape
 * the counter executors emit: one event per recipient, `amount` counters each.
 */
class CountersPlacedBatchTriggerTest : FunSpec({

    val heroesYouControl = GameObjectFilter.Creature.youControl().withSubtype(Subtype.HERO)

    // "Whenever you put one or more +1/+1 counters on one or more OTHER Heroes you control …"
    val batchObserver = card("Batch Counter Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Hero"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.another(heroesYouControl).getsCounters(CounterType.PLUS_ONE_PLUS_ONE, by = Player.You, batch = true)
            effect = Effects.DrawCards(1)
        }
    }

    // The per-permanent twin: identical in every respect except `batch`.
    val perPermanentObserver = card("Per Permanent Counter Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Hero"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.another(heroesYouControl).getsCounters(CounterType.PLUS_ONE_PLUS_ONE, by = Player.You)
            effect = Effects.DrawCards(1)
        }
    }

    // Same batch pattern, but keeping the printed "for the first time this turn" rider — the axis
    // that narrows a batch to the placements that were each the first on their recipient this turn.
    val firstTimeBatchObserver = card("First Time Batch Counter Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Hero"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.another(heroesYouControl).getsCounters(CounterType.PLUS_ONE_PLUS_ONE, by = Player.You, firstTimeEachTurn = true, batch = true)
            effect = Effects.DrawCards(1)
        }
    }

    // The SELF-bound batch: "whenever you put one or more counters on ~". Unlike the ANY-only
    // tap/untap batch passes, the counters batch honors all three bindings.
    val selfBatchObserver = card("Self Batch Counter Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Hero"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.matching(heroesYouControl).getsCounters(CounterType.PLUS_ONE_PLUS_ONE, by = Player.You, batch = true)
            effect = Effects.DrawCards(1)
        }
    }

    val hero = card("Batch Test Hero") {
        manaCost = "{W}"
        typeLine = "Creature — Human Hero"
        power = 1
        toughness = 1
    }

    val bystander = card("Batch Test Bystander") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                batchObserver,
                perPermanentObserver,
                firstTimeBatchObserver,
                selfBatchObserver,
                hero,
                bystander,
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        return driver
    }

    /** The pending triggers whose ability is the batched (or per-permanent) counter observer. */
    fun countersTriggersOf(driver: GameTestDriver, events: List<CountersAddedEvent>, sourceId: EntityId) =
        TriggerDetector(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null), conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions)
            .detectTriggers(driver.state, events)
            .filter { it.ability.trigger is EventPattern.CountersPlacedEvent && it.sourceId == sourceId }

    fun placed(
        entityId: EntityId,
        amount: Int,
        placedBy: EntityId?,
        counterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
        firstThisTurn: Boolean = true,
    ) = CountersAddedEvent(
        entityId = entityId,
        counterType = counterType,
        amount = amount,
        entityName = "",
        firstThisTurn = firstThisTurn,
        // A single-kind placement here, so the per-kind window tracks the any-kind one.
        firstOfTypeThisTurn = firstThisTurn,
        placedBy = placedBy,
    )

    context("CountersPlacedEvent(batch = true)") {

        test("one counter on one other Hero fires the batch trigger once") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver, listOf(placed(heroId, 1, driver.player1)), observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.triggeringEntityId shouldBe heroId
        }

        /**
         * The discriminating case. One effect putting a counter on three Heroes emits three
         * [CountersAddedEvent]s in one detection pass; the batch template collapses them to a single
         * firing while the per-permanent twin fires three times off the very same events.
         */
        test("counters on three other Heroes from one effect fire the batch trigger once, not three times") {
            val driver = createDriver()
            val batchId = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val perPermanentId =
                driver.putCreatureOnBattlefield(driver.player1, "Per Permanent Counter Observer")
            val heroA = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")
            val heroB = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")
            val heroC = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val events = listOf(
                placed(heroA, 1, driver.player1),
                placed(heroB, 1, driver.player1),
                placed(heroC, 1, driver.player1),
            )

            val batchTriggers = countersTriggersOf(driver, events, batchId)
            withClue("batch template fires once for the whole placement (CR 603.2c)") {
                batchTriggers shouldHaveSize 1
            }
            withClue("per-permanent template still fires once per recipient off the same events") {
                countersTriggersOf(driver, events, perPermanentId) shouldHaveSize 3
            }
            withClue("all three recipients are exposed as the captured collection") {
                batchTriggers.first().triggerContext.capturedEntityIds shouldBe listOf(heroA, heroB, heroC)
            }
        }

        /**
         * The captured collection is what a "for each of those creatures" payoff counts, so it must
         * hold the *matching* recipients only — the same narrowing the firing itself gets.
         */
        test("the captured collection holds the matching recipients only") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val bystanderId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Bystander")
            val heroA = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")
            val heroB = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver,
                listOf(
                    placed(bystanderId, 1, driver.player1),
                    placed(heroA, 1, driver.player1),
                    placed(heroB, 1, driver.player1),
                ),
                observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.capturedEntityIds shouldBe listOf(heroA, heroB)
        }

        test("several counters on one Hero at once fire the batch trigger once and report the batch total") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver, listOf(placed(heroId, 3, driver.player1)), observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.counterCount shouldBe 3
        }

        test("counterCount sums the whole batch across recipients") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroA = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")
            val heroB = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver,
                listOf(placed(heroA, 2, driver.player1), placed(heroB, 1, driver.player1)),
                observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.counterCount shouldBe 3
        }

        /**
         * A batch mixing matching and non-matching recipients is *narrowed*, not discarded: it still
         * fires once, and the triggering entity bound for any "it" payoff is a matching one.
         */
        test("a batch of one Hero and one non-Hero fires once and binds the Hero") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val bystanderId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Bystander")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver,
                // Non-matching recipient first, so a detector that just took `events.first()` fails.
                listOf(placed(bystanderId, 1, driver.player1), placed(heroId, 1, driver.player1)),
                observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.triggeringEntityId shouldBe heroId
        }

        test("a batch of only non-Heroes does not fire it") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val bystanderId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Bystander")

            countersTriggersOf(
                driver, listOf(placed(bystanderId, 1, driver.player1)), observer
            ) shouldHaveSize 0
        }

        test("counters landing only on the observer itself do not fire an OTHER-bound batch trigger") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")

            // The observer is itself a Hero you control — only the OTHER binding keeps it out.
            countersTriggersOf(
                driver, listOf(placed(observer, 2, driver.player1)), observer
            ) shouldHaveSize 0
        }

        test("counters an opponent put on your Hero do not fire a 'you put' batch trigger") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            countersTriggersOf(
                driver, listOf(placed(heroId, 1, driver.player2)), observer
            ) shouldHaveSize 0
        }

        test("an unattributed placement does not fire a 'you put' batch trigger") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            countersTriggersOf(
                driver, listOf(placed(heroId, 1, placedBy = null)), observer
            ) shouldHaveSize 0
        }

        test("counters of another type do not fire a +1/+1 batch trigger") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            countersTriggersOf(
                driver,
                listOf(placed(heroId, 1, driver.player1, counterType = CounterType.MINUS_ONE_MINUS_ONE)),
                observer
            ) shouldHaveSize 0
        }

        test("a batch mixing counter types fires once, on the matching placements alone") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroA = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")
            val heroB = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver,
                listOf(
                    placed(heroA, 4, driver.player1, counterType = CounterType.MINUS_ONE_MINUS_ONE),
                    placed(heroB, 1, driver.player1),
                ),
                observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.triggeringEntityId shouldBe heroB
            withClue("the -1/-1 counters are not part of this trigger's batch") {
                triggers.first().triggerContext.counterCount shouldBe 1
            }
        }

        /**
         * A placement reduced to zero (a replacement effect can do that — see
         * `ReplacementEffectUtils.applyCounterPlacementModifiers`) is not a placement, and the two
         * multiplicities must agree about that: the per-permanent twin has to stay silent too, the
         * way the mirror-image `CountersRemovedEvent` path already does for a zero-amount removal.
         */
        test("a placement of zero counters fires neither multiplicity") {
            val driver = createDriver()
            val batchId = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val perPermanentId =
                driver.putCreatureOnBattlefield(driver.player1, "Per Permanent Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val events = listOf(placed(heroId, 0, driver.player1))

            withClue("batch template") {
                countersTriggersOf(driver, events, batchId) shouldHaveSize 0
            }
            withClue("per-permanent template") {
                countersTriggersOf(driver, events, perPermanentId) shouldHaveSize 0
            }
        }

        /**
         * A zero-amount placement bundled with a real one narrows the batch rather than poisoning
         * it: the trigger still fires once, and the batch total counts only the real counters.
         */
        test("a zero-counter placement in a batch is dropped from the total, not from the firing") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroA = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")
            val heroB = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver,
                listOf(placed(heroA, 0, driver.player1), placed(heroB, 2, driver.player1)),
                observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.triggeringEntityId shouldBe heroB
            triggers.first().triggerContext.counterCount shouldBe 2
            triggers.first().triggerContext.capturedEntityIds shouldBe listOf(heroB)
        }

        test("firstTimeEachTurn narrows a batch to the placements that were the first this turn") {
            val driver = createDriver()
            val observer =
                driver.putCreatureOnBattlefield(driver.player1, "First Time Batch Counter Observer")
            val heroA = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")
            val heroB = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver,
                listOf(
                    placed(heroA, 5, driver.player1, firstThisTurn = false),
                    placed(heroB, 1, driver.player1, firstThisTurn = true),
                ),
                observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.triggeringEntityId shouldBe heroB
            withClue("the already-countered Hero's placement is not part of this batch") {
                triggers.first().triggerContext.counterCount shouldBe 1
            }
        }

        test("a firstTimeEachTurn batch of only repeat placements does not fire") {
            val driver = createDriver()
            val observer =
                driver.putCreatureOnBattlefield(driver.player1, "First Time Batch Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            countersTriggersOf(
                driver, listOf(placed(heroId, 1, driver.player1, firstThisTurn = false)), observer
            ) shouldHaveSize 0
        }

        test("a SELF-bound batch fires on counters put on the observer itself") {
            val driver = createDriver()
            val observer =
                driver.putCreatureOnBattlefield(driver.player1, "Self Batch Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val triggers = countersTriggersOf(
                driver,
                // The other Hero's counters are not this observer's business, SELF-bound.
                listOf(placed(heroId, 4, driver.player1), placed(observer, 2, driver.player1)),
                observer
            )

            triggers shouldHaveSize 1
            triggers.first().triggerContext.triggeringEntityId shouldBe observer
            triggers.first().triggerContext.counterCount shouldBe 2
        }

        test("a SELF-bound batch does not fire on counters put on another Hero") {
            val driver = createDriver()
            val observer =
                driver.putCreatureOnBattlefield(driver.player1, "Self Batch Counter Observer")
            val heroId = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            countersTriggersOf(
                driver, listOf(placed(heroId, 1, driver.player1)), observer
            ) shouldHaveSize 0
        }

        /**
         * Two effects placing counters in the same turn are two separate detection passes, hence two
         * separate batches — the trigger fires once for each rather than being collapsed across them.
         */
        test("counters placed by two different effects fire it once each") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Batch Counter Observer")
            val heroA = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")
            val heroB = driver.putCreatureOnBattlefield(driver.player1, "Batch Test Hero")

            val firstPass = countersTriggersOf(
                driver, listOf(placed(heroA, 1, driver.player1), placed(heroB, 1, driver.player1)), observer
            )
            val secondPass = countersTriggersOf(
                driver, listOf(placed(heroA, 1, driver.player1), placed(heroB, 1, driver.player1)), observer
            )

            firstPass shouldHaveSize 1
            secondPass shouldHaveSize 1
        }
    }
})
