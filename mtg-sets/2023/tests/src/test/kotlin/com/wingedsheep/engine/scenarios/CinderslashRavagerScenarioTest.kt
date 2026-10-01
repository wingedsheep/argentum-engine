package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.CinderslashRavager
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cinderslash Ravager (ONE #200) — {4}{R}{G} 5/5 Creature — Phyrexian Warrior.
 *
 * "This spell costs {1} less to cast for each permanent you control with oil counters on it.
 *  Vigilance. When this creature enters, it deals 1 damage to each creature your opponents control."
 */
class CinderslashRavagerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CinderslashRavager))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun setOil(driver: GameTestDriver, id: EntityId, count: Int) {
        driver.replaceState(driver.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.OIL to count))) })
    }

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 10) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("costs {1} less per oil permanent (not per counter) and pings each opposing creature on entry") {
        val driver = newDriver()
        val p1 = driver.player1
        val p2 = driver.player2

        // Two permanents with oil counters (one carrying three) -> reduction of exactly {2}.
        setOil(driver, driver.putPermanentOnBattlefield(p1, "Forest"), 3)
        setOil(driver, driver.putCreatureOnBattlefield(p1, "Centaur Courser"), 1)
        driver.putPermanentOnBattlefield(p1, "Forest") // no counters — doesn't count

        val myLions = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        val theirLions = driver.putCreatureOnBattlefield(p2, "Savannah Lions")
        val theirCourser = driver.putCreatureOnBattlefield(p2, "Centaur Courser")

        val ravager = driver.putCardInHand(p1, "Cinderslash Ravager")
        driver.giveMana(p1, Color.RED, 1)
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, ravager).outcome shouldBe Outcome.Done
        resolveAll(driver)

        val onField = driver.findPermanent(p1, "Cinderslash Ravager")
        onField shouldNotBe null
        driver.state.projectedState.hasKeyword(onField!!, Keyword.VIGILANCE) shouldBe true

        // Opponent's 2/1 dies, their 3/3 survives; our own 2/1 is untouched.
        driver.findPermanent(p2, "Savannah Lions") shouldBe null
        driver.getGraveyard(p2).contains(theirLions) shouldBe true
        driver.findPermanent(p2, "Centaur Courser") shouldBe theirCourser
        driver.findPermanent(p1, "Savannah Lions") shouldBe myLions
    }

    test("a single oil permanent is not enough to cast it for four mana") {
        val driver = newDriver()
        val p1 = driver.player1
        setOil(driver, driver.putPermanentOnBattlefield(p1, "Forest"), 2)

        val ravager = driver.putCardInHand(p1, "Cinderslash Ravager")
        driver.giveMana(p1, Color.RED, 1)
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, ravager).outcome shouldNotBe Outcome.Done
        driver.findPermanent(p1, "Cinderslash Ravager") shouldBe null
    }
})
