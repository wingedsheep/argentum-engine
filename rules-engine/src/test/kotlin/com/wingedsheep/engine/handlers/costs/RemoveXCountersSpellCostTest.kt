package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.engine.state.ZoneKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Remove X counters from among permanents you control" as a spell's additional cost, with X
 * announced independently of the mana cost and floored on the flashback route — Light Up the
 * Night's "Flashback—{3}{R}, Remove X loyalty counters from among planeswalkers you control. If you
 * cast this spell this way, X can't be 0."
 */
class RemoveXCountersSpellCostTest : FunSpec({
    // {X} from hand; flashback {1} plus "remove X +1/+1 counters from among creatures you control",
    // X can't be 0 that way. Gains X life so the resolved X is observable.
    val counterFlashback = card("Counter Flashback Spell") {
        manaCost = "{X}"
        typeLine = "Sorcery"
        keywordAbility(
            KeywordAbility.flashback(
                "{1}",
                Costs.additional.RemoveXCounters(CounterType.PLUS_ONE_PLUS_ONE, GameObjectFilter.Creature),
                minimumXValue = 1,
            )
        )
        spell { effect = Effects.GainLife(DynamicAmount.XValue) }
    }

    fun driver(): EnumerationTestDriver = EnumerationTestDriver().also {
        it.registerCards(TestCards.all + counterFlashback)
        it.game.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.game.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun EnumerationTestDriver.counters(entity: EntityId, type: CounterType = CounterType.PLUS_ONE_PLUS_ONE): Int =
        game.state.getEntity(entity)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun EnumerationTestDriver.setCounters(entity: EntityId, vararg counters: Pair<CounterType, Int>) =
        game.replaceState(game.state.updateEntity(entity) { it.with(CountersComponent(mapOf(*counters))) })

    fun flashback(d: EnumerationTestDriver, card: EntityId, x: Int?, vararg removals: Pair<EntityId, Int>) = CastSpell(
        d.player1, card, xValue = x,
        useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK,
        additionalCostPayment = AdditionalCostPayment(
            distributedCounterRemovals = removals.map { (id, n) -> DistributedCounterRemoval(id, "+1/+1", n) }
        ),
        paymentStrategy = PaymentStrategy.FromPool,
    )

    test("the flashback offer announces X, capped by the matching counters you control, floored at 1") {
        val d = driver()
        d.game.putCardInGraveyard(d.player1, counterFlashback.name)
        val bearA = d.game.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val bearB = d.game.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.game.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.setCounters(bearA, CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.of("charge") to 4)
        d.setCounters(bearB, CounterType.PLUS_ONE_PLUS_ONE to 1)
        d.setCounters(theirs, CounterType.PLUS_ONE_PLUS_ONE to 5)
        d.game.giveColorlessMana(d.player1, 1)

        val offer = d.enumerateFor(d.player1).castActionsFor(counterFlashback.name)
            .single { (it.action as CastSpell).alternativeCostType == AlternativeCostType.FLASHBACK }
        offer.affordable shouldBe true
        offer.hasXCost shouldBe true
        // Only +1/+1 counters, only on creatures you control: 2 + 1.
        offer.maxAffordableX shouldBe 3
        offer.minX shouldBe 1
        offer.additionalCostInfo!!.counterRemovalCreatures.map { it.entityId }.toSet() shouldBe setOf(bearA, bearB)
    }

    test("X removed counters, split across permanents, pay the cost and become the spell's X") {
        val d = driver()
        val spell = d.game.putCardInGraveyard(d.player1, counterFlashback.name)
        val bearA = d.game.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val bearB = d.game.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.setCounters(bearA, CounterType.PLUS_ONE_PLUS_ONE to 2)
        d.setCounters(bearB, CounterType.PLUS_ONE_PLUS_ONE to 1)
        d.game.giveColorlessMana(d.player1, 1)

        val result = d.game.submit(flashback(d, spell, 2, bearA to 1, bearB to 1))
        result.error shouldBe null
        result.events.filterIsInstance<CountersRemovedEvent>().sumOf { it.amount } shouldBe 2
        d.counters(bearA) shouldBe 1
        d.counters(bearB) shouldBe 0

        d.game.bothPass()
        d.game.getLifeTotal(d.player1) shouldBe 22
        // Cast with flashback, so it is exiled rather than returned to the graveyard.
        (spell in d.game.state.getZone(ZoneKey(d.player1, Zone.EXILE))) shouldBe true
    }

    test("removals that don't total exactly X are rejected without changing the game") {
        for (removals in listOf(arrayOf(1), arrayOf(2, 1))) {
            val d = driver()
            val spell = d.game.putCardInGraveyard(d.player1, counterFlashback.name)
            val bearA = d.game.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val bearB = d.game.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            d.setCounters(bearA, CounterType.PLUS_ONE_PLUS_ONE to 2)
            d.setCounters(bearB, CounterType.PLUS_ONE_PLUS_ONE to 1)
            d.game.giveColorlessMana(d.player1, 1)
            val before = d.game.state
            val paid = listOf(bearA, bearB).zip(removals.toList()).toTypedArray()
            d.game.submit(flashback(d, spell, 2, *paid)).error shouldNotBe null
            d.game.state shouldBe before
        }
    }

    test("an opponent's counters can't pay the cost") {
        val d = driver()
        val spell = d.game.putCardInGraveyard(d.player1, counterFlashback.name)
        val theirs = d.game.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.setCounters(theirs, CounterType.PLUS_ONE_PLUS_ONE to 3)
        d.game.giveColorlessMana(d.player1, 1)
        d.game.submit(flashback(d, spell, 1, theirs to 1)).error shouldNotBe null
        d.counters(theirs) shouldBe 3
    }

    test("X can't be 0 on the flashback route, so no counters means no flashback cast") {
        val d = driver()
        val spell = d.game.putCardInGraveyard(d.player1, counterFlashback.name)
        d.game.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.game.giveColorlessMana(d.player1, 1)
        d.enumerateFor(d.player1).castActionsFor(counterFlashback.name)
            .filter { (it.action as CastSpell).alternativeCostType == AlternativeCostType.FLASHBACK }
            .none { it.affordable } shouldBe true
        val before = d.game.state
        d.game.submit(flashback(d, spell, 0)).error shouldNotBe null
        d.game.state shouldBe before
    }

    test("the floor is flashback's alone: the hand cast still allows X = 0 and owes no counters") {
        val d = driver()
        val spell = d.game.putCardInHand(d.player1, counterFlashback.name)
        val bear = d.game.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.setCounters(bear, CounterType.PLUS_ONE_PLUS_ONE to 2)
        val offer = d.enumerateFor(d.player1).castActionsFor(counterFlashback.name).single()
        offer.minX shouldBe 0
        d.game.submit(CastSpell(d.player1, spell, xValue = 0, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.counters(bear) shouldBe 2
    }
})
