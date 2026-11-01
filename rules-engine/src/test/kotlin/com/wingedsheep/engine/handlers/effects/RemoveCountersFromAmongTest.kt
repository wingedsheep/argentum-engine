package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `RemoveCountersFromAmongEffect` — "you may remove any number of <kind> counters from among
 * <filter>", recording the total removed for a composed follow-up.
 *
 * The test spell gains its caster life equal to the stored total, so the recorded count is
 * observable. Covers: eligibility (filter, controller, counter kind), per-permanent caps, an empty
 * answer, an over-cap answer rejected, removal events, and the 0 stored when nothing is eligible.
 */
class RemoveCountersFromAmongTest : FunSpec({

    val harvest = card("Counter Harvest Test") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.RemoveCountersFromAmong(
                CounterType.PLUS_ONE_PLUS_ONE,
                GameObjectFilter.Creature.youControl(),
                storeAmountAs = "removed"
            ) then Effects.GainLife(DynamicAmounts.storedNumber("removed"))
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(harvest))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingLife = 20)
        return d
    }

    fun GameTestDriver.counters(id: EntityId, kind: CounterType = CounterType.PLUS_ONE_PLUS_ONE): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0

    fun GameTestDriver.castHarvest(player: EntityId) {
        val spell = putCardInHand(player, "Counter Harvest Test")
        giveMana(player, Color.GREEN, 1)
        castSpell(player, spell).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("offers only your permanents carrying the kind, each capped at its own count") {
        val d = driver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val a = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val wrongKind = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.addComponent(a, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        d.addComponent(b, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3, CounterType.CHARGE to 1)))
        d.addComponent(wrongKind, CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1)))
        d.addComponent(theirs, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))

        d.castHarvest(you)

        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.targets shouldContainExactlyInAnyOrder listOf(a, b)
        decision.maxPerTarget shouldBe mapOf(a to 1, b to 3)
        decision.totalAmount shouldBe 4
        decision.minPerTarget shouldBe 0
        decision.allowPartial shouldBe true

        // A share above a permanent's own count is rejected and the decision stays open.
        d.submitDecision(you, DistributionResponse(decision.id, mapOf(a to 2))).error shouldNotBe null
        d.pendingDecision shouldBe decision

        d.submitDecision(you, DistributionResponse(decision.id, mapOf(a to 1, b to 2)))

        d.counters(a) shouldBe 0
        d.counters(b) shouldBe 1
        d.counters(b, CounterType.CHARGE) shouldBe 1
        d.counters(theirs) shouldBe 2
        d.getLifeTotal(you) shouldBe 23

        val removals = d.events.filterIsInstance<CountersRemovedEvent>()
        removals.map { it.entityId to it.amount } shouldContainExactlyInAnyOrder listOf(a to 1, b to 2)
        removals.all { it.removedBy == you && it.counterType == CounterType.PLUS_ONE_PLUS_ONE } shouldBe true
    }

    test("an empty answer removes nothing and stores 0") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val a = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.addComponent(a, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))

        d.castHarvest(you)
        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        d.submitDecision(you, DistributionResponse(decision.id, emptyMap()))

        d.counters(a) shouldBe 2
        d.getLifeTotal(you) shouldBe 20
        d.events.filterIsInstance<CountersRemovedEvent>() shouldBe emptyList()
    }

    test("nothing eligible resolves without a prompt and stores 0") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(you, "Grizzly Bears")

        d.castHarvest(you)

        d.pendingDecision shouldBe null
        d.getLifeTotal(you) shouldBe 20
    }
})
