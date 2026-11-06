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
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `MoveChosenCountersToTarget` from a `GroupRef` source, narrowed by `counterType` — "move any number
 * of +1/+1 counters from other permanents you control onto this creature" (Aetherborn Marauder).
 * Driven through an inline creature with that enters trigger:
 *  - one prompt per other permanent carrying the kind, bounded 0..its count; each answer moves
 *    exactly that many, and zero is a legal answer;
 *  - other counter kinds never move and raise no prompt;
 *  - permanents without the kind are never asked about;
 *  - a destination that has left the battlefield before resolution receives nothing, and no
 *    counter leaves a source;
 *  - the gathered counters are put on the destination as one placement per kind, after the last
 *    answer (Hardened Scales' one-extra is pinned in AetherbornMarauderScenarioTest).
 */
class MoveChosenCountersFromGroupTest : FunSpec({

    val gatherer = card("Test Gatherer") {
        manaCost = "{B}"
        colorIdentity = "B"
        typeLine = "Creature — Rogue"
        power = 1
        toughness = 1
        oracleText = "When this creature enters, move any number of +1/+1 counters from other " +
            "permanents you control onto this creature."
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.MoveChosenCountersToTarget(
                source = EffectTarget.GroupRef(
                    GroupFilter(
                        GameObjectFilter.Permanent.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
                    ).other()
                ),
                destination = EffectTarget.Self,
                counterType = CounterType.PLUS_ONE_PLUS_ONE
            )
        }
    }

    fun GameTestDriver.addCounters(entityId: EntityId, type: CounterType, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        })
    }

    fun GameTestDriver.count(entityId: EntityId, type: CounterType): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(type) ?: 0

    data class Board(
        val driver: GameTestDriver,
        val courser: EntityId,
        val bears: EntityId,
        val bystander: EntityId,
        val gathererId: EntityId
    )

    /**
     * Courser with three +1/+1 and two charge counters, Bears with two +1/+1, a counterless
     * bystander; Test Gatherer cast and resolved, its enters trigger on the stack.
     */
    fun castGatherer(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(gatherer))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val bystander = driver.putCreatureOnBattlefield(active, "Savannah Lions")
        driver.addCounters(courser, CounterType.PLUS_ONE_PLUS_ONE, 3)
        driver.addCounters(courser, CounterType.CHARGE, 2)
        driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)
        val spell = driver.putCardInHand(active, "Test Gatherer")
        driver.giveMana(active, Color.BLACK, 1)
        driver.castSpell(active, spell).error shouldBe null
        driver.bothPass() // creature resolves; enters trigger goes on the stack
        val gathererId = driver.findPermanent(active, "Test Gatherer")!!
        return Board(driver, courser, bears, bystander, gathererId)
    }

    /** Answer the pending prompt for whichever source it names from [answers]. */
    fun GameTestDriver.answer(answers: Map<EntityId, Int>): EntityId {
        val decision = pendingDecision
        decision.shouldBeInstanceOf<ChooseNumberDecision>()
        val source = decision.context.sourceId!!
        decision.minValue shouldBe 0
        submitDecision(activePlayer!!, NumberChosenResponse(decision.id, answers.getValue(source))).error shouldBe null
        return source
    }

    test("asks once per other permanent with +1/+1 counters and moves exactly what was chosen") {
        val (driver, courser, bears, _, gathererId) = castGatherer()
        driver.bothPass() // trigger resolves

        val maxBySource = mapOf(courser to 3, bears to 2)
        val asked = (1..2).map {
            val decision = driver.pendingDecision
            decision.shouldBeInstanceOf<ChooseNumberDecision>()
            decision.maxValue shouldBe maxBySource.getValue(decision.context.sourceId!!)
            driver.answer(mapOf(courser to 2, bears to 2))
        }

        driver.pendingDecision shouldBe null
        asked.toSet() shouldBe setOf(courser, bears)
        driver.count(gathererId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        driver.count(courser, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.count(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        // The other kind on the Courser was never offered and never moved.
        driver.count(courser, CounterType.CHARGE) shouldBe 2
        driver.count(gathererId, CounterType.CHARGE) shouldBe 0
    }

    test("choosing zero everywhere moves nothing") {
        val (driver, courser, bears, _, gathererId) = castGatherer()
        driver.bothPass()

        driver.answer(mapOf(courser to 0, bears to 0))
        driver.answer(mapOf(courser to 0, bears to 0))

        driver.pendingDecision shouldBe null
        driver.count(gathererId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        driver.count(courser, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        driver.count(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
    }

    test("a destination that left the battlefield before resolution takes nothing") {
        val (driver, courser, bears, _, gathererId) = castGatherer()
        driver.moveToGraveyard(gathererId)
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.count(courser, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        driver.count(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        driver.count(gathererId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }
})
