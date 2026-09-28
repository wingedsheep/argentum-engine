package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Damage-observer bucket routing: a permanent holding both a "deals damage to you" observer
 * (`Recipient.You`, the damage-to-you bucket) and a general damage observer is filed under both
 * buckets. Each detector must act only on the abilities of its own bucket, or the You observer
 * fires once per bucket (Elesh Norn's two damage triggers).
 */
class DamageObserverBucketRoutingTest : FunSpec({

    val observer = card("Bucket Routing Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Any.opponentControls()).dealsDamage(Recipient.You)
            effect = Effects.DrawCards(1)
        }
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Any.opponentControls()).dealsDamage(Recipient.PermanentYouControl)
            effect = Effects.DrawCards(1)
        }
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(observer))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20))
        return driver
    }

    fun detectorFor(driver: GameTestDriver) = TriggerDetector(
        driver.cardRegistry,
        conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions,
        predicateEvaluator = PredicateEvaluator(cardRegistry = null)
    )

    fun TriggerDetector.observerTriggers(driver: GameTestDriver, events: List<DamageDealtEvent>) =
        detectTriggers(driver.state, events).filter { it.ability.trigger is com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent }

    test("damage to you fires the You observer exactly once and not the general one") {
        val driver = setup()
        driver.putCreatureOnBattlefield(driver.player1, "Bucket Routing Observer")
        val source = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        val triggers = detectorFor(driver).observerTriggers(
            driver,
            listOf(DamageDealtEvent(sourceId = source, targetId = driver.player1, amount = 2, isCombatDamage = false, targetIsPlayer = true))
        )

        triggers shouldHaveSize 1
        (triggers.single().ability.trigger as com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent).recipient shouldBe Recipient.You
    }

    test("damage to a permanent you control fires the general observer exactly once and not the You one") {
        val driver = setup()
        driver.putCreatureOnBattlefield(driver.player1, "Bucket Routing Observer")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val source = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        val triggers = detectorFor(driver).observerTriggers(
            driver,
            listOf(DamageDealtEvent(sourceId = source, targetId = mine, amount = 2, isCombatDamage = false, targetIsPlayer = false))
        )

        triggers shouldHaveSize 1
        (triggers.single().ability.trigger as com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent).recipient shouldBe Recipient.PermanentYouControl
    }

    test("damage to you and to a permanent you control fires each observer exactly once") {
        val driver = setup()
        driver.putCreatureOnBattlefield(driver.player1, "Bucket Routing Observer")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val source = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        val triggers = detectorFor(driver).observerTriggers(
            driver,
            listOf(
                DamageDealtEvent(sourceId = source, targetId = driver.player1, amount = 2, isCombatDamage = false, targetIsPlayer = true),
                DamageDealtEvent(sourceId = source, targetId = mine, amount = 2, isCombatDamage = false, targetIsPlayer = false),
            )
        )

        triggers shouldHaveSize 2
    }
})
