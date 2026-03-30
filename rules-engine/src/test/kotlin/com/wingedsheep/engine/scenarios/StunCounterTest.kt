package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for stun counter mechanics (CR 122.1b):
 * If a permanent with a stun counter would become untapped, instead remove a stun counter from it.
 */
class StunCounterTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun addStunCounters(driver: GameTestDriver, entityId: EntityId, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.STUN, count))
        }
        driver.replaceState(newState)
    }

    fun getStunCounters(driver: GameTestDriver, entityId: EntityId): Int {
        return driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0
    }

    test("permanent with stun counter does not untap during untap step - counter is removed instead") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a tapped creature with a stun counter
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.tapPermanent(bears)
        addStunCounters(driver, bears, 1)

        driver.isTapped(bears) shouldBe true
        getStunCounters(driver, bears) shouldBe 1

        // Pass to next turn's main phase (goes through untap step)
        driver.passPriorityUntil(Step.END)
        driver.passPriority(activePlayer)
        driver.passPriority(driver.getOpponent(activePlayer))
        // Now it's opponent's turn - pass through to our next untap
        driver.passPriorityUntil(Step.END)
        driver.passPriority(driver.getOpponent(activePlayer))
        driver.passPriority(activePlayer)
        // Back to our turn - untap step already happened
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Creature should still be tapped but stun counter removed
        driver.isTapped(bears) shouldBe true
        getStunCounters(driver, bears) shouldBe 0
    }

    test("permanent with multiple stun counters loses one per untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a tapped creature with 3 stun counters
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.tapPermanent(bears)
        addStunCounters(driver, bears, 3)

        getStunCounters(driver, bears) shouldBe 3

        // Pass through one full turn cycle
        driver.passPriorityUntil(Step.END)
        driver.passPriority(activePlayer)
        driver.passPriority(driver.getOpponent(activePlayer))
        driver.passPriorityUntil(Step.END)
        driver.passPriority(driver.getOpponent(activePlayer))
        driver.passPriority(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Should have lost one stun counter, still tapped
        driver.isTapped(bears) shouldBe true
        getStunCounters(driver, bears) shouldBe 2
    }

    test("permanent without stun counters untaps normally") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a tapped creature WITHOUT stun counters
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.tapPermanent(bears)

        driver.isTapped(bears) shouldBe true

        // Pass through one full turn cycle
        driver.passPriorityUntil(Step.END)
        driver.passPriority(activePlayer)
        driver.passPriority(driver.getOpponent(activePlayer))
        driver.passPriorityUntil(Step.END)
        driver.passPriority(driver.getOpponent(activePlayer))
        driver.passPriority(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Should untap normally
        driver.isTapped(bears) shouldBe false
    }
})
