package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.EvolvedSpinoderm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Evolved Spinoderm (ONE #166) — {2}{G}{G} 5/5 Creature — Phyrexian Beast.
 *
 * "This creature enters with four oil counters on it. This creature has trample as long as it has
 *  two or fewer oil counters on it. Otherwise, it has hexproof. At the beginning of your upkeep,
 *  remove an oil counter from this creature. Then if it has no oil counters on it, sacrifice it."
 */
class EvolvedSpinodermScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EvolvedSpinoderm))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun setOil(driver: GameTestDriver, id: EntityId, count: Int) {
        driver.replaceState(driver.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.OIL to count))) })
    }

    fun castSpinoderm(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Evolved Spinoderm")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        driver.bothPass()
        return driver.findPermanent(p1, "Evolved Spinoderm")!!
    }

    fun toNextUpkeep(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.DRAW)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.activePlayerId shouldBe driver.player1
        driver.state.stack.size shouldBe 1
    }

    fun hasTrample(driver: GameTestDriver, id: EntityId) = driver.state.projectedState.hasKeyword(id, Keyword.TRAMPLE)
    fun hasHexproof(driver: GameTestDriver, id: EntityId) = driver.state.projectedState.hasKeyword(id, Keyword.HEXPROOF)

    test("enters with four oil counters, has hexproof and not trample") {
        val driver = newDriver()
        val spino = castSpinoderm(driver)
        oil(driver, spino) shouldBe 4
        hasHexproof(driver, spino) shouldBe true
        hasTrample(driver, spino) shouldBe false
    }

    test("three oil counters still hexproof; two switches to trample") {
        val driver = newDriver()
        val spino = castSpinoderm(driver)
        setOil(driver, spino, 3)
        hasHexproof(driver, spino) shouldBe true
        hasTrample(driver, spino) shouldBe false

        setOil(driver, spino, 2)
        hasHexproof(driver, spino) shouldBe false
        hasTrample(driver, spino) shouldBe true
    }

    test("upkeep trigger removes one oil counter") {
        val driver = newDriver()
        val spino = castSpinoderm(driver)

        toNextUpkeep(driver)
        driver.bothPass()

        oil(driver, spino) shouldBe 3
        driver.findPermanent(driver.player1, "Evolved Spinoderm") shouldBe spino
    }

    test("removing the last oil counter sacrifices it") {
        val driver = newDriver()
        val spino = castSpinoderm(driver)
        setOil(driver, spino, 1)

        toNextUpkeep(driver)
        driver.bothPass()

        driver.findPermanent(driver.player1, "Evolved Spinoderm") shouldBe null
        driver.assertInGraveyard(driver.player1, "Evolved Spinoderm")
    }
})
