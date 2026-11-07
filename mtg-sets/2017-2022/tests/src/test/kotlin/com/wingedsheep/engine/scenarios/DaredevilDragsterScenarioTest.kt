package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.DaredevilDragster
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Daredevil Dragster (AER #149) — the end-of-combat trigger adds a velocity counter only when the
 * Vehicle attacked or blocked this combat, and the "two or more" check is made as the trigger
 * resolves: the second counter sacrifices it and draws two cards.
 */
class DaredevilDragsterScenarioTest : FunSpec({
    fun setup(startingVelocity: Int = 0): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + DaredevilDragster)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val dragster = driver.putPermanentOnBattlefield(driver.player1, "Daredevil Dragster")
        driver.removeSummoningSickness(dragster)
        if (startingVelocity > 0) {
            driver.replaceState(driver.state.updateEntity(dragster) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withCounters(CounterType.VELOCITY, startingVelocity))
            })
        }
        val crew = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.submitSuccess(CrewVehicle(driver.player1, dragster, listOf(crew)))
        driver.bothPass()
        return driver to dragster
    }

    fun GameTestDriver.velocity(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.VELOCITY) ?: 0

    fun GameTestDriver.attackAndResolveEndOfCombat(dragster: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        submitSuccess(DeclareAttackers(player1, mapOf(dragster to player2)))
        passPriorityUntil(Step.END_COMBAT)
        bothPass()
    }

    test("attacking puts one velocity counter on it and it stays") {
        val (driver, dragster) = setup()
        val handBefore = driver.getHandSize(driver.player1)
        driver.attackAndResolveEndOfCombat(dragster)
        driver.velocity(dragster) shouldBe 1
        driver.state.getBattlefield().contains(dragster) shouldBe true
        driver.getHandSize(driver.player1) shouldBe handBefore
    }

    test("the second velocity counter sacrifices it and draws two cards") {
        val (driver, dragster) = setup(startingVelocity = 1)
        val handBefore = driver.getHandSize(driver.player1)
        driver.attackAndResolveEndOfCombat(dragster)
        driver.state.getBattlefield().contains(dragster) shouldBe false
        driver.assertInGraveyard(driver.player1, "Daredevil Dragster")
        driver.getHandSize(driver.player1) shouldBe handBefore + 2
    }

    test("no counter when it neither attacked nor blocked") {
        val (driver, dragster) = setup(startingVelocity = 1)
        val handBefore = driver.getHandSize(driver.player1)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.velocity(dragster) shouldBe 1
        driver.state.getBattlefield().contains(dragster) shouldBe true
        driver.getHandSize(driver.player1) shouldBe handBefore
    }
})
