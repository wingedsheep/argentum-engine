package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.LatticeBladeMantis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lattice-Blade Mantis (ONE #173) — {3}{G} 4/3 Creature — Phyrexian Insect.
 *
 * "This creature enters with two oil counters on it. Whenever this creature attacks, you may remove
 *  an oil counter from it. If you do, untap it and it gets +1/+1 until end of turn."
 */
class LatticeBladeMantisScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LatticeBladeMantis))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun setOil(driver: GameTestDriver, id: EntityId, count: Int) {
        driver.replaceState(driver.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.OIL to count))) })
    }

    fun castMantis(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Lattice-Blade Mantis")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveColorlessMana(p1, 3)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        driver.bothPass()
        val mantis = driver.findPermanent(p1, "Lattice-Blade Mantis")!!
        driver.removeSummoningSickness(mantis)
        return mantis
    }

    fun attackAndAnswer(driver: GameTestDriver, mantis: EntityId, accept: Boolean) {
        val p1 = driver.player1
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(mantis), driver.getOpponent(p1)).outcome shouldBe Outcome.Done
        var asked = false
        var safety = 0
        while (safety < 20) {
            val pending = driver.state.pendingDecision
            if (pending is YesNoDecision) {
                asked = true
                driver.submitYesNo(pending.playerId, accept)
            } else if (driver.stackSize > 0) {
                driver.bothPass()
            } else {
                break
            }
            safety++
        }
        asked shouldBe true
    }

    test("enters with two oil counters") {
        val driver = newDriver()
        val mantis = castMantis(driver)
        oil(driver, mantis) shouldBe 2
        driver.state.projectedState.getPower(mantis) shouldBe 4
        driver.state.projectedState.getToughness(mantis) shouldBe 3
    }

    test("accepting removes an oil counter, untaps it, and gives +1/+1") {
        val driver = newDriver()
        val mantis = castMantis(driver)
        attackAndAnswer(driver, mantis, accept = true)

        oil(driver, mantis) shouldBe 1
        driver.isTapped(mantis) shouldBe false
        driver.state.projectedState.getPower(mantis) shouldBe 5
        driver.state.projectedState.getToughness(mantis) shouldBe 4
    }

    test("declining keeps the counters, stays tapped, no pump") {
        val driver = newDriver()
        val mantis = castMantis(driver)
        attackAndAnswer(driver, mantis, accept = false)

        oil(driver, mantis) shouldBe 2
        driver.isTapped(mantis) shouldBe true
        driver.state.projectedState.getPower(mantis) shouldBe 4
    }

    test("with no oil counters left, accepting does nothing") {
        val driver = newDriver()
        val mantis = castMantis(driver)
        setOil(driver, mantis, 0)
        attackAndAnswer(driver, mantis, accept = true)

        oil(driver, mantis) shouldBe 0
        driver.isTapped(mantis) shouldBe true
        driver.state.projectedState.getPower(mantis) shouldBe 4
    }
})
