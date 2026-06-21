package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.ScriedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SurveiledEvent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Substrate tests for the "Whenever you surveil" (CR 701.42) and combined "Whenever you scry or
 * surveil" (CR 701.18 / 701.42) triggers: `Patterns.Library.surveil(N)` ends by emitting
 * [SurveiledEvent], which drives `Triggers.WheneverYouSurveil` /
 * `Triggers.WheneverYouScryOrSurveil` and surfaces "the number of cards looked at" via
 * [ContextPropertyKey.TRIGGER_SCRY_COUNT]. The event is distinct from the scry event, so a scry
 * never fires a surveil trigger (and vice versa) — proven by the isolation test.
 */
class SurveilTriggerScenarioTest : FunSpec({

    fun surveilSpell(name: String, n: Int) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Surveil $n."
        spell { effect = Patterns.Library.surveil(n) }
    }
    fun scrySpell(name: String, n: Int) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Scry $n."
        spell { effect = Patterns.Library.scry(n) }
    }
    val SurveilOne = surveilSpell("Surveil One", 1)
    val SurveilThree = surveilSpell("Surveil Three", 3)
    val SurveilZero = surveilSpell("Surveil Zero", 0)
    val ScryOne = scrySpell("Scry One", 1)

    // "Whenever you surveil, put a +1/+1 counter on it." — fires once per surveil.
    val SurveilWatcher = card("Surveil Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.WheneverYouSurveil
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // "Whenever you scry, put a +1/+1 counter on it." — must NOT fire on a surveil.
    val ScryWatcher = card("Scry Watcher Iso") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.WheneverYouScry
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // "Whenever you scry or surveil, put a +1/+1 counter on it." — fires on both.
    val BothWatcher = card("Look Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.WheneverYouScryOrSurveil
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // "Whenever you surveil, put X +1/+1 counters on it, where X is the number of cards looked at."
    val SurveilCounter = card("Surveil Counter") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.WheneverYouSurveil
            effect = Effects.AddDynamicCounters(
                Counters.PLUS_ONE_PLUS_ONE,
                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_SCRY_COUNT),
                EffectTarget.Self
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                SurveilOne, SurveilThree, SurveilZero, ScryOne,
                SurveilWatcher, ScryWatcher, BothWatcher, SurveilCounter
            )
        )
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // Truncate a player's library to exactly [size] cards so surveil can be exercised against a
    // library shorter than N (CR 701.42a) or empty (701.42d).
    fun GameTestDriver.truncateLibrary(player: EntityId, size: Int) {
        val key = ZoneKey(player, Zone.LIBRARY)
        replaceState(state.copy(zones = state.zones + (key to state.getZone(key).take(size))))
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 10) bothPass()
    }

    // Cast the named scry/surveil spell, then drive all resolution-time decisions:
    // - SelectCardsDecision ("put any number into graveyard / on the bottom") → choose none
    // - ReorderLibraryDecision ("in what order on top") → keep printed order
    fun GameTestDriver.castLook(player: EntityId, cardName: String) {
        val cardId = putCardInHand(player, cardName)
        castSpell(player, cardId)
        bothPass()
        repeat(4) {
            when (val decision = pendingDecision) {
                is SelectCardsDecision ->
                    submitDecision(player, CardsSelectedResponse(decision.id, emptyList()))
                is ReorderLibraryDecision ->
                    submitDecision(player, OrderedResponse(decision.id, decision.cards))
                else -> return
            }
        }
    }

    test("surveil emits a SurveiledEvent with the count looked at") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val before = driver.events.size
        driver.castLook(active, "Surveil Three")
        val events = driver.events.drop(before).filterIsInstance<SurveiledEvent>()

        events.size shouldBe 1
        events.single().playerId shouldBe active
        events.single().count shouldBe 3
    }

    test("Whenever-you-surveil trigger fires once per surveil") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Surveil Watcher")
        driver.castLook(active, "Surveil One")
        driver.bothPass()
        driver.plusOneCounters(watcher) shouldBe 1

        driver.castLook(active, "Surveil Three")
        driver.bothPass()
        driver.plusOneCounters(watcher) shouldBe 2
    }

    test("TRIGGER_SCRY_COUNT delivers the cards-looked-at count to a surveil trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val counter = driver.putCreatureOnBattlefield(active, "Surveil Counter")

        driver.castLook(active, "Surveil Three")
        driver.bothPass()
        driver.plusOneCounters(counter) shouldBe 3

        driver.castLook(active, "Surveil One")
        driver.bothPass()
        driver.plusOneCounters(counter) shouldBe 4
    }

    test("scry-or-surveil trigger fires on both a scry and a surveil") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Look Watcher")

        driver.castLook(active, "Scry One")
        driver.bothPass()
        driver.plusOneCounters(watcher) shouldBe 1 // fired on the scry

        driver.castLook(active, "Surveil One")
        driver.bothPass()
        driver.plusOneCounters(watcher) shouldBe 2 // ... and on the surveil
    }

    test("scry and surveil triggers don't cross-fire (distinct events)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val surveilWatcher = driver.putCreatureOnBattlefield(active, "Surveil Watcher")
        val scryWatcher = driver.putCreatureOnBattlefield(active, "Scry Watcher Iso")

        // A scry fires only the scry watcher.
        driver.castLook(active, "Scry One")
        driver.bothPass()
        driver.plusOneCounters(scryWatcher) shouldBe 1
        driver.plusOneCounters(surveilWatcher) shouldBe 0

        // A surveil fires only the surveil watcher.
        driver.castLook(active, "Surveil One")
        driver.bothPass()
        driver.plusOneCounters(surveilWatcher) shouldBe 1
        driver.plusOneCounters(scryWatcher) shouldBe 1 // unchanged
    }

    test("surveil counts only the cards actually looked at when the library is shorter than N (CR 701.42a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val counter = driver.putCreatureOnBattlefield(active, "Surveil Counter")
        driver.truncateLibrary(active, 2) // only two cards available to a "surveil 3"

        val before = driver.events.size
        driver.castLook(active, "Surveil Three")
        driver.bothPass()

        val surveiled = driver.events.drop(before).filterIsInstance<SurveiledEvent>().single()
        surveiled.count shouldBe 2
        driver.plusOneCounters(counter) shouldBe 2
    }

    test("surveil with an empty library still fires the trigger with count 0 (CR 701.42d)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Surveil Watcher")
        val counter = driver.putCreatureOnBattlefield(active, "Surveil Counter")
        driver.truncateLibrary(active, 0) // empty library: zero cards looked at, but you still surveil

        val before = driver.events.size
        driver.castLook(active, "Surveil Three")
        driver.resolveStack()

        val surveiled = driver.events.drop(before).filterIsInstance<SurveiledEvent>().single()
        surveiled.count shouldBe 0
        driver.plusOneCounters(watcher) shouldBe 1 // trigger fired (701.42d)
        driver.plusOneCounters(counter) shouldBe 0 // ... but scaled to 0 cards looked at
    }

    test("surveil 0 fires no trigger and emits no event (CR 701.42c)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Surveil Watcher")

        val before = driver.events.size
        driver.castLook(active, "Surveil Zero")
        driver.bothPass()

        driver.events.drop(before).filterIsInstance<SurveiledEvent>() shouldBe emptyList()
        driver.events.drop(before).filterIsInstance<ScriedEvent>() shouldBe emptyList()
        driver.plusOneCounters(watcher) shouldBe 0
    }
})
