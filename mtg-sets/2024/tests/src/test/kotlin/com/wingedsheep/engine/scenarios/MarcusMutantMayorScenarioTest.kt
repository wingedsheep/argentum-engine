package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.MarcusMutantMayor
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Marcus, Mutant Mayor (PIP #109).
 *
 * "Whenever a creature you control deals combat damage to a player, draw a card if that creature
 * has a +1/+1 counter on it. If it doesn't, put a +1/+1 counter on it."
 *
 * The branch reads the *triggering* creature's counters at resolution: an unmodified attacker
 * gets a counter (no card), an attacker already carrying a +1/+1 counter draws (no new counter).
 */
class MarcusMutantMayorScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MarcusMutantMayor))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusCounters(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.attackWithBear(withCounter: Boolean): Pair<EntityId, Int> {
        val me = activePlayer!!
        putCreatureOnBattlefield(me, "Marcus, Mutant Mayor")
        val bear = putCreatureOnBattlefield(me, "Grizzly Bears")
        if (withCounter) addComponent(bear, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        removeSummoningSickness(bear)

        passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = getHandSize(me)
        declareAttackers(me, listOf(bear), getOpponent(me))
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
        return bear to handBefore
    }

    test("an attacker without a +1/+1 counter gets one and draws nothing") {
        val driver = newGame()
        val (bear, handBefore) = driver.attackWithBear(withCounter = false)

        driver.plusCounters(bear) shouldBe 1
        driver.getHandSize(driver.activePlayer!!) shouldBe handBefore
    }

    test("an attacker with a +1/+1 counter draws a card and gets no new counter") {
        val driver = newGame()
        val (bear, handBefore) = driver.attackWithBear(withCounter = true)

        driver.plusCounters(bear) shouldBe 1
        driver.getHandSize(driver.activePlayer!!) shouldBe handBefore + 1
    }
})
