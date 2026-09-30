package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `Effects.MoveCounterOfAnyKind` — the floored, budgeted form of `MoveChosenCountersToTarget`
 * (`minTotal = maxTotal = count`). Driven through an inline instant that moves two counters, so
 * the budget-and-floor walk has room to force, ask, and stop:
 *  - kinds that can only just cover the floor are moved without a prompt;
 *  - a prompt's minimum is the share of the floor the later kinds can't cover, and an answer
 *    below it is rejected;
 *  - the walk stops once the budget is spent;
 *  - a source carrying fewer than `count` gives up everything it has.
 */
class MoveCounterOfAnyKindTest : FunSpec({

    val transfer = card("Test Transfer") {
        manaCost = "{U}"
        colorIdentity = "U"
        typeLine = "Instant"
        oracleText = "Move two counters from target creature onto another target creature."
        spell {
            val from = target(TargetFilter.Creature)
            val onto = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
            effect = Effects.MoveCounterOfAnyKind(source = from, destination = onto, count = 2)
        }
    }

    val kinds = listOf(CounterType.PLUS_ONE_PLUS_ONE, CounterType.CHARGE, CounterType.STUN)

    fun GameTestDriver.addCounters(entityId: EntityId, type: CounterType, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        })
    }

    fun GameTestDriver.count(entityId: EntityId, type: CounterType): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun GameTestDriver.total(entityId: EntityId): Int = kinds.sumOf { count(entityId, it) }

    /** Board with two creatures, the spell cast targeting them, and the spell resolving. */
    fun castTransfer(setupCounters: GameTestDriver.(from: EntityId) -> Unit): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(transfer))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val from = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val onto = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.setupCounters(from)
        val spell = driver.putCardInHand(active, "Test Transfer")
        driver.giveMana(active, Color.BLUE, 1)
        driver.castSpell(active, spell, listOf(from, onto)).error shouldBe null
        driver.bothPass()
        return Triple(driver, from, onto)
    }

    test("two kinds of one counter each exactly cover the floor, so both move without a prompt") {
        val (driver, from, onto) = castTransfer {
            addCounters(it, CounterType.PLUS_ONE_PLUS_ONE, 1)
            addCounters(it, CounterType.CHARGE, 1)
        }

        driver.pendingDecision shouldBe null
        driver.count(onto, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.count(onto, CounterType.CHARGE) shouldBe 1
        driver.total(from) shouldBe 0
    }

    test("declining the first kind forces the floor onto the kinds after it") {
        val (driver, from, onto) = castTransfer { id -> kinds.forEach { addCounters(id, it, 1) } }

        val first = driver.pendingDecision
        first.shouldBeInstanceOf<ChooseNumberDecision>()
        first.minValue shouldBe 0
        first.maxValue shouldBe 1
        driver.submitDecision(driver.activePlayer!!, NumberChosenResponse(first.id, 0))

        driver.pendingDecision shouldBe null
        driver.total(onto) shouldBe 2
        driver.total(from) shouldBe 1
    }

    test("the walk stops once the budget is spent") {
        val (driver, from, onto) = castTransfer {
            addCounters(it, CounterType.PLUS_ONE_PLUS_ONE, 3)
            addCounters(it, CounterType.CHARGE, 3)
        }

        val first = driver.pendingDecision
        first.shouldBeInstanceOf<ChooseNumberDecision>()
        first.minValue shouldBe 0
        first.maxValue shouldBe 2
        driver.submitDecision(driver.activePlayer!!, NumberChosenResponse(first.id, 2))

        driver.pendingDecision shouldBe null
        driver.total(onto) shouldBe 2
        driver.total(from) shouldBe 4
    }

    test("an answer below the prompt's floor is rejected") {
        val (driver, from, onto) = castTransfer {
            addCounters(it, CounterType.PLUS_ONE_PLUS_ONE, 3)
            addCounters(it, CounterType.CHARGE, 1)
        }

        // Charge can cover at most one of the two, so +1/+1 must supply at least one.
        val first = driver.pendingDecision
        first.shouldBeInstanceOf<ChooseNumberDecision>()
        first.minValue shouldBe 1
        driver.submitDecision(driver.activePlayer!!, NumberChosenResponse(first.id, 0)).error shouldNotBe null
        driver.total(onto) shouldBe 0

        driver.submitDecision(driver.activePlayer!!, NumberChosenResponse(first.id, 1)).error shouldBe null
        driver.pendingDecision shouldBe null
        driver.total(onto) shouldBe 2
        driver.total(from) shouldBe 2
    }

    test("a source with fewer counters than the count gives up all it has") {
        val (driver, from, onto) = castTransfer { addCounters(it, CounterType.CHARGE, 1) }

        driver.pendingDecision shouldBe null
        driver.count(onto, CounterType.CHARGE) shouldBe 1
        driver.total(from) shouldBe 0
    }
})
