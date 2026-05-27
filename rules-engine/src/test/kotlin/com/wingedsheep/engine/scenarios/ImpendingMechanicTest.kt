package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mechanic-level tests for Impending (CR 702.176), wired by the `impending(n, cost)` DSL helper.
 *
 * A permanent cast for its impending cost enters with N time counters, isn't a creature while it
 * has one, and loses a time counter at the beginning of its controller's end step — becoming a
 * creature once the last is removed. Cast for its normal mana cost, it adds no time counters and
 * behaves as an ordinary enchantment creature.
 */
class ImpendingMechanicTest : FunSpec({

    // One time counter — its full lifecycle (enter → first end step → creature) fits in one turn.
    val impendingOne = card("Impending One") {
        manaCost = "{3}{B}{B}"
        typeLine = "Enchantment Creature — Horror"
        power = 5
        toughness = 5
        impending(1, "{1}{B}")
    }

    // Five time counters — mirrors Overlord of the Balemurk's "enters with five time counters".
    val impendingFive = card("Impending Five") {
        manaCost = "{3}{B}{B}"
        typeLine = "Enchantment Creature — Horror"
        power = 5
        toughness = 5
        impending(5, "{1}{B}")
    }

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(impendingOne, impendingFive))
        return driver
    }

    fun timeCounters(driver: GameTestDriver, perm: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(perm)?.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0

    test("a creature can be cast for its impending cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Impending Five")
        driver.giveMana(player, Color.BLACK, 2)

        val result = driver.submit(
            CastSpell(player, cardId, useAlternativeCost = true, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
        driver.stackSize shouldBe 1
    }

    test("cast for impending, it enters with N time counters and isn't a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Impending Five")
        driver.giveMana(player, Color.BLACK, 2)
        driver.submit(
            CastSpell(player, cardId, useAlternativeCost = true, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        val perm = driver.findPermanent(player, "Impending Five")
        perm shouldNotBe null
        timeCounters(driver, perm!!) shouldBe 5
        projector.project(driver.state).isCreature(perm) shouldBe false
        // It's still an enchantment while impending.
        projector.project(driver.state).hasType(perm, "ENCHANTMENT") shouldBe true
    }

    test("a time counter is removed at the controller's end step; it becomes a creature when the last is gone") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Impending One")
        driver.giveMana(player, Color.BLACK, 2)
        driver.submit(
            CastSpell(player, cardId, useAlternativeCost = true, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        val perm = driver.findPermanent(player, "Impending One")!!
        timeCounters(driver, perm) shouldBe 1
        projector.project(driver.state).isCreature(perm) shouldBe false

        // Advance to the controller's end step; the remove-a-time-counter trigger goes on the stack.
        driver.passPriorityUntil(Step.END)
        driver.bothPass() // resolve the impending end-step trigger

        timeCounters(driver, perm) shouldBe 0
        projector.project(driver.state).isCreature(perm) shouldBe true
    }

    test("cast for its normal mana cost, it enters as a creature with no time counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Impending Five")
        driver.giveMana(player, Color.BLACK, 5)
        driver.submit(
            CastSpell(player, cardId, useAlternativeCost = false, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        val perm = driver.findPermanent(player, "Impending Five")!!
        timeCounters(driver, perm) shouldBe 0
        projector.project(driver.state).isCreature(perm) shouldBe true
    }
})
