package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ScryBottomCountScenarioTest : FunSpec({
    fun spell(name: String, effect: Effect) = card(name) {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { this.effect = effect }
    }
    fun watcher(name: String, pause: Boolean = false) = card(name) {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.scries()
            triggerRestriction = Conditions.CompareAmounts(
                DynamicAmounts.triggerScryBottomCount(), ComparisonOperator.GT, DynamicAmount.Fixed(0)
            )
            effect = if (pause) Effects.Surveil(1) then Effects.GainLife(DynamicAmounts.triggerScryBottomCount())
                else Effects.GainLife(DynamicAmounts.triggerScryBottomCount())
        }
    }
    val three = spell("Bottom Count Scry", Effects.Scry(3))
    val twice = spell("Bottom Count Twice", Effects.Scry(3) then Effects.Scry(3))
    val zero = spell("Bottom Count Zero", Effects.Scry(0))
    val surveil = spell("Bottom Count Surveil", Effects.Surveil(3))
    val watch = watcher("Bottom Count Watcher")
    val pauseWatch = watcher("Bottom Count Pausing Watcher", true)
    fun setup(pausing: Boolean = false) = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(three, twice, zero, surveil, watch, pauseWatch))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        putPermanentOnBattlefield(activePlayer!!, if (pausing) pauseWatch.name else watch.name)
    }
    fun GameTestDriver.roundTrip() {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        replaceState(json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), state)))
    }
    fun GameTestDriver.start(name: String = three.name) {
        val me = priorityPlayer!!
        castSpell(me, putCardInHand(me, name)).outcome shouldBe Outcome.Done
        bothPass()
    }
    fun GameTestDriver.choose(count: Int) {
        val decision = pendingDecision as SelectCardsDecision
        submitDecision(decision.playerId, CardsSelectedResponse(decision.id, decision.options.take(count)))
        // The count must survive the separate top-ordering pause and a persisted game state.
        if (pendingDecision is ReorderLibraryDecision) {
            roundTrip()
            val order = pendingDecision as ReorderLibraryDecision
            submitDecision(order.playerId, OrderedResponse(order.id, order.cards))
        }
    }
    for (bottom in listOf(0, 1, 3)) {
        test("scry 3 captures $bottom chosen bottom cards separately from cards looked at") {
            val d = setup(); val me = d.activePlayer!!
            d.start(); d.choose(bottom)
            val event = d.events.filterIsInstance<ScriedEvent>().single()
            event.count shouldBe 3
            event.bottomCount shouldBe bottom
            d.stackSize shouldBe if (bottom > 0) 1 else 0
            if (bottom > 0) d.bothPass()
            d.getLifeTotal(me) shouldBe 20 + bottom
        }
    }
    test("short library counts only the cards selected, empty library and scry zero do not trigger") {
        for (size in listOf(0, 1)) {
            val d = setup(); val me = d.activePlayer!!; val key = ZoneKey(me, Zone.LIBRARY)
            d.replaceState(d.state.copy(zones = d.state.zones + (key to d.state.getZone(key).take(size))))
            d.start()
            if (size > 0) d.choose(1)
            d.events.filterIsInstance<ScriedEvent>().single().bottomCount shouldBe size
            d.stackSize shouldBe size
            if (size > 0) d.bothPass()
            d.getLifeTotal(me) shouldBe 20 + size
            val before = d.events.filterIsInstance<ScriedEvent>().size
            d.start(zero.name)
            d.stackSize shouldBe 0
            d.events.filterIsInstance<ScriedEvent>().size shouldBe before
        }
    }
    test("two scries in one resolution retain independent bottom counts") {
        val d = setup(); val me = d.activePlayer!!
        d.start(twice.name)
        d.choose(1)
        d.choose(2)
        d.events.filterIsInstance<ScriedEvent>().map { it.bottomCount } shouldBe listOf(1, 2)
        // Simultaneous triggers may ask their controller for stack order.
        (d.pendingDecision as? OrderObjectsDecision)?.let {
            d.submitDecision(it.playerId, OrderedResponse(it.id, it.objects))
        }
        repeat(2) { d.bothPass() }
        d.getLifeTotal(me) shouldBe 23
    }
    test("the captured amount survives a decision inside the triggered effect") {
        val d = setup(pausing = true); val me = d.activePlayer!!
        d.start(); d.choose(3)
        d.roundTrip()
        d.bothPass()
        d.roundTrip()
        d.choose(0) // Surveil inside the trigger, then read the original scry's bottom count.
        d.getLifeTotal(me) shouldBe 23
    }
    test("opponent scry and surveil do not fire the controller's scry-bottom trigger") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.start(surveil.name); d.choose(3)
        d.stackSize shouldBe 0
        d.passPriority(me)
        d.start(); d.choose(2)
        d.events.filterIsInstance<ScriedEvent>().single().playerId shouldBe opp
        d.stackSize shouldBe 0
        d.getLifeTotal(me) shouldBe 20
    }
})
