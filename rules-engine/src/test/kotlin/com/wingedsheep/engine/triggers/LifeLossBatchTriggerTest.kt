package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for batched life-loss triggers — `EventPattern.LifeLossEvent(batch = true)`,
 * optionally narrowed by `exactAmount`, detected by `TriggerDetector.detectLifeLossBatchTriggers`.
 *
 *  - "Whenever one or more opponents each lose exactly 1 life" (Ob Nixilis, Captive Kingpin): each
 *    opponent's losses in the batch are summed — the Ob Nixilis ruling that three 1/1s dealing
 *    combat damage at once are a loss of 3 — and the trigger fires once if any opponent's total is
 *    exactly 1. Damage, "loses N life" and a life payment all count (the "pays exactly 1 life"
 *    ruling).
 *  - "Whenever one or more opponents lose life": once per occurrence however many opponents lost life.
 *  - An action's damage is one simultaneous occurrence; other losses split into runs that end when a
 *    player repeats, so "loses 1 life, then loses 1 life" is two occurrences.
 *  - The per-event `LifeLossEvent` (no batch) keeps firing once per event.
 *
 * Driven directly with synthesized [LifeChangedEvent]s in a three-player game.
 */
class LifeLossBatchTriggerTest : FunSpec({

    val exactlyOneObserver = card("Exactly One Life Loss Observer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.anOpponent.losesLife(exactly = 1)
            effect = Effects.DrawCards(1)
        }
    }

    val oneOrMoreObserver = card("One Or More Life Loss Observer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.anOpponent.losesLife(batch = true)
            effect = Effects.DrawCards(1)
        }
    }

    val perEventObserver = card("Per Event Life Loss Observer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.anOpponent.losesLife()
            effect = Effects.DrawCards(1)
        }
    }

    class Table(val driver: GameTestDriver, val players: List<EntityId>) {
        val you get() = players[0]
        val oppA get() = players[1]
        val oppB get() = players[2]
    }

    fun table(): Table {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(exactlyOneObserver, oneOrMoreObserver, perEventObserver))
        val players = driver.initMultiplayer(decks = List(3) { Deck.of("Plains" to 40) })
        return Table(driver, players)
    }

    fun loss(player: EntityId, amount: Int, reason: LifeChangeReason = LifeChangeReason.DAMAGE) =
        LifeChangedEvent(player, 20, 20 - amount, reason)

    fun Table.triggersOf(observer: String, events: List<GameEvent>): List<com.wingedsheep.engine.event.PendingTrigger> {
        val sourceId = driver.putPermanentOnBattlefield(you, observer)
        return TriggerDetector(
            driver.cardRegistry,
            predicateEvaluator = PredicateEvaluator(cardRegistry = null),
            conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions
        ).detectTriggers(driver.state, events)
            .filter { it.ability.trigger is EventPattern.LifeLossEvent && it.sourceId == sourceId }
    }

    context("each lose exactly N life") {

        test("an opponent losing exactly 1 life fires once, bound to that opponent and amount") {
            val t = table()
            val fired = t.triggersOf("Exactly One Life Loss Observer", listOf(loss(t.oppA, 1)))
            fired shouldHaveSize 1
            fired.single().triggerContext.triggeringPlayerId shouldBe t.oppA
            fired.single().triggerContext.damageAmount shouldBe 1
        }

        test("simultaneous 1-point losses to one opponent are summed — combat from three 1/1s doesn't fire") {
            val t = table()
            t.triggersOf(
                "Exactly One Life Loss Observer",
                listOf(loss(t.oppA, 1), loss(t.oppA, 1), loss(t.oppA, 1))
            ).shouldBeEmpty()
        }

        test("losing 2 life doesn't fire") {
            val t = table()
            t.triggersOf("Exactly One Life Loss Observer", listOf(loss(t.oppA, 2))).shouldBeEmpty()
        }

        test("one opponent losing exactly 1 while another loses 3 fires once") {
            val t = table()
            val fired = t.triggersOf("Exactly One Life Loss Observer", listOf(loss(t.oppA, 3), loss(t.oppB, 1)))
            fired shouldHaveSize 1
            fired.single().triggerContext.triggeringPlayerId shouldBe t.oppB
        }

        test("two opponents each losing exactly 1 fires once, not once per opponent") {
            val t = table()
            t.triggersOf(
                "Exactly One Life Loss Observer",
                listOf(loss(t.oppA, 1, LifeChangeReason.LIFE_LOSS), loss(t.oppB, 1, LifeChangeReason.LIFE_LOSS))
            ) shouldHaveSize 1
        }

        test("losing 1 life, then 1 life again in one resolution is two occurrences — fires twice") {
            val t = table()
            t.triggersOf(
                "Exactly One Life Loss Observer",
                listOf(loss(t.oppA, 1, LifeChangeReason.LIFE_LOSS), loss(t.oppA, 1, LifeChangeReason.LIFE_LOSS))
            ) shouldHaveSize 2
        }

        test("1 damage and a separate loss of 1 in one resolution fire once each") {
            val t = table()
            t.triggersOf(
                "Exactly One Life Loss Observer",
                listOf(loss(t.oppA, 1), loss(t.oppA, 1, LifeChangeReason.LIFE_LOSS))
            ) shouldHaveSize 2
        }

        test("paying exactly 1 life counts") {
            val t = table()
            t.triggersOf(
                "Exactly One Life Loss Observer",
                listOf(loss(t.oppA, 1, LifeChangeReason.PAYMENT))
            ) shouldHaveSize 1
        }

        test("the controller losing 1 life doesn't fire an opponents trigger") {
            val t = table()
            t.triggersOf("Exactly One Life Loss Observer", listOf(loss(t.you, 1))).shouldBeEmpty()
        }

        test("life gain is not life loss") {
            val t = table()
            t.triggersOf(
                "Exactly One Life Loss Observer",
                listOf(LifeChangedEvent(t.oppA, 20, 21, LifeChangeReason.LIFE_GAIN))
            ).shouldBeEmpty()
        }
    }

    context("one or more lose life") {

        test("several opponents dealt damage at once fires once") {
            val t = table()
            t.triggersOf(
                "One Or More Life Loss Observer",
                listOf(loss(t.oppA, 2), loss(t.oppB, 1), loss(t.oppA, 4))
            ) shouldHaveSize 1
        }

        test("no opponent losing life doesn't fire") {
            val t = table()
            t.triggersOf("One Or More Life Loss Observer", listOf(loss(t.you, 2))).shouldBeEmpty()
        }

        test("the per-event reading still fires once per loss event") {
            val t = table()
            t.triggersOf(
                "Per Event Life Loss Observer",
                listOf(loss(t.oppA, 2), loss(t.oppB, 1), loss(t.oppA, 4))
            ) shouldHaveSize 3
        }
    }

    test("an exact amount needs the batch reading") {
        shouldThrow<IllegalArgumentException> {
            EventPattern.LifeLossEvent(Player.EachOpponent, batch = false, exactAmount = 1)
        }
        Triggers.anOpponent.losesLife(exactly = 1).event shouldBe
            EventPattern.LifeLossEvent(Player.EachOpponent, batch = true, exactAmount = 1)
        EventPattern.LifeLossEvent(Player.EachOpponent, batch = true, exactAmount = 1).description shouldBe
            "one or more opponents each lose exactly 1 life"
    }
})
