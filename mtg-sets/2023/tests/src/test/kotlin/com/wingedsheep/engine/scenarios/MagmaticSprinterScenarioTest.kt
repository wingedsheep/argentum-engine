package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.MagmaticSprinter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Magmatic Sprinter (ONE #140) — {2}{R} 3/2 Creature — Phyrexian Warrior.
 *
 * "Haste
 *  When this creature enters, put two oil counters on target artifact or creature you control.
 *  At the beginning of your end step, return this creature to its owner's hand unless you remove
 *  two oil counters from it."
 */
class MagmaticSprinterScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MagmaticSprinter))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun seedOil(driver: GameTestDriver, id: EntityId, amount: Int) {
        driver.replaceState(driver.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, amount))
        })
    }

    test("has haste; ETB puts two oil counters on target creature you control") {
        val driver = newDriver()
        val p1 = driver.player1
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val card = driver.putCardInHand(p1, "Magmatic Sprinter")
        driver.giveMana(p1, Color.RED, 1)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.pendingDecision == null && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.submitTargetSelection(p1, listOf(bears)).error shouldBe null
        guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val sprinter = driver.findPermanent(p1, "Magmatic Sprinter")
        sprinter shouldNotBe null
        driver.state.projectedState.hasKeyword(sprinter!!, Keyword.HASTE) shouldBe true
        oil(driver, bears) shouldBe 2
        oil(driver, sprinter) shouldBe 0
    }

    test("end step: with fewer than two oil counters it returns to hand without a prompt") {
        val driver = newDriver()
        val p1 = driver.player1
        val sprinter = driver.putCreatureOnBattlefield(p1, "Magmatic Sprinter")
        seedOil(driver, sprinter, 1)

        driver.passPriorityUntil(Step.END)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) {
            (driver.pendingDecision is YesNoDecision) shouldBe false
            driver.bothPass()
        }

        driver.findPermanent(p1, "Magmatic Sprinter") shouldBe null
        driver.state.getZone(p1, Zone.HAND).any { driver.getCardName(it) == "Magmatic Sprinter" } shouldBe true
    }

    test("end step: removing two oil counters keeps it on the battlefield") {
        val driver = newDriver()
        val p1 = driver.player1
        val sprinter = driver.putCreatureOnBattlefield(p1, "Magmatic Sprinter")
        seedOil(driver, sprinter, 3)

        driver.passPriorityUntil(Step.END)
        var guard = 0
        var prompted = false
        while (guard++ < 10) {
            when {
                driver.pendingDecision is YesNoDecision -> {
                    prompted = true
                    driver.submitYesNo(p1, true)
                }
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }

        prompted shouldBe true
        driver.findPermanent(p1, "Magmatic Sprinter") shouldBe sprinter
        oil(driver, sprinter) shouldBe 1
    }

    test("end step: declining to remove the counters returns it to hand") {
        val driver = newDriver()
        val p1 = driver.player1
        val sprinter = driver.putCreatureOnBattlefield(p1, "Magmatic Sprinter")
        seedOil(driver, sprinter, 2)

        driver.passPriorityUntil(Step.END)
        var guard = 0
        var prompted = false
        while (guard++ < 10) {
            when {
                driver.pendingDecision is YesNoDecision -> {
                    prompted = true
                    driver.submitYesNo(p1, false)
                }
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }

        prompted shouldBe true
        driver.findPermanent(p1, "Magmatic Sprinter") shouldBe null
        driver.state.getZone(p1, Zone.HAND).any { driver.getCardName(it) == "Magmatic Sprinter" } shouldBe true
    }
})
