package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.ScriedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Substrate tests for the "Whenever you scry" trigger (CR 701.22):
 * `EffectPatterns.scry(N)` ends by emitting [ScriedEvent], which drives
 * `Triggers.WheneverYouScry` and surfaces "the number of cards looked at" via
 * [ContextPropertyKey.TRIGGER_SCRY_COUNT].
 */
class ScryTriggerScenarioTest : FunSpec({

    // Sorcery that scries N — substrate driver.
    fun scrySpell(name: String, n: Int) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Scry $n."
        spell { effect = EffectPatterns.scry(n) }
    }
    val ScryOne = scrySpell("Scry One", 1)
    val ScryThree = scrySpell("Scry Three", 3)

    // "Whenever you scry, put a +1/+1 counter on Scry Watcher." — fires once per scry,
    // regardless of how many cards were looked at.
    val ScryWatcher = card("Scry Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.WheneverYouScry
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // "Whenever you scry, put X +1/+1 counters on Scry Counter, where X is the number of
    // cards looked at." — proves TRIGGER_SCRY_COUNT carries the scry N through to resolution.
    val ScryCounter = card("Scry Counter") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.WheneverYouScry
            effect = Effects.AddDynamicCounters(
                Counters.PLUS_ONE_PLUS_ONE,
                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_SCRY_COUNT),
                EffectTarget.Self
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ScryOne, ScryThree, ScryWatcher, ScryCounter))
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // Cast the named scry spell, then drive all resolution-time decisions:
    // - SelectCardsDecision for "put any number on the bottom" → choose none
    // - ReorderLibraryDecision for "in what order on top" → keep printed order
    // Stops once the spell finishes resolving (no scry-related decision pending).
    fun GameTestDriver.castScry(player: EntityId, cardName: String) {
        val cardId = putCardInHand(player, cardName)
        castSpell(player, cardId)
        bothPass()
        repeat(4) {
            val decision = pendingDecision
            when (decision) {
                is SelectCardsDecision ->
                    submitDecision(player, CardsSelectedResponse(decision.id, emptyList()))
                is ReorderLibraryDecision ->
                    submitDecision(player, OrderedResponse(decision.id, decision.cards))
                else -> return
            }
        }
    }

    test("scry emits a ScriedEvent with the count looked at") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val before = driver.events.size
        driver.castScry(active, "Scry Three")
        val scriedEvents = driver.events.drop(before).filterIsInstance<ScriedEvent>()

        scriedEvents.size shouldBe 1
        scriedEvents.single().playerId shouldBe active
        scriedEvents.single().count shouldBe 3
    }

    test("Whenever-you-scry trigger fires once per scry") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Scry Watcher")
        driver.castScry(active, "Scry One")
        driver.bothPass()
        driver.plusOneCounters(watcher) shouldBe 1

        driver.castScry(active, "Scry Three")
        driver.bothPass()
        driver.plusOneCounters(watcher) shouldBe 2
    }

    test("TRIGGER_SCRY_COUNT delivers the cards-looked-at count to the trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val counter = driver.putCreatureOnBattlefield(active, "Scry Counter")

        driver.castScry(active, "Scry Three")
        driver.bothPass()
        driver.plusOneCounters(counter) shouldBe 3

        driver.castScry(active, "Scry One")
        driver.bothPass()
        driver.plusOneCounters(counter) shouldBe 4
    }
})
