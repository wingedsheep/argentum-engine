package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.WatchfulBlisterzoa
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Watchful Blisterzoa (ONE #78) — {4}{U}{U} 4/4 Creature — Phyrexian Jellyfish.
 *
 * Flying; enters with an oil counter; when it dies, draw cards equal to the number of oil
 * counters on it (last-known information).
 */
class WatchfulBlisterzoaScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WatchfulBlisterzoa))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castBlisterzoa(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val zoa = driver.putCardInHand(player, "Watchful Blisterzoa")
        driver.giveMana(player, Color.BLUE, 6)
        driver.castSpell(player, zoa).error shouldBe null
        driver.bothPass()
        return zoa
    }

    fun doomBlade(driver: GameTestDriver, victim: EntityId) {
        val player = driver.player1
        val blade = driver.putCardInHand(player, "Doom Blade")
        driver.giveMana(player, Color.BLACK, 2)
        driver.castSpell(player, blade, listOf(victim)).outcome shouldBe Outcome.Done
        driver.bothPass() // Doom Blade resolves; dies trigger goes on the stack
        driver.bothPass() // dies trigger resolves
    }

    test("enters with one oil counter and flying") {
        val driver = newDriver()
        val zoa = castBlisterzoa(driver)
        oil(driver, zoa) shouldBe 1
        driver.state.projectedState.hasKeyword(zoa, Keyword.FLYING) shouldBe true
    }

    test("dies with one oil counter: draws one card") {
        val driver = newDriver()
        val zoa = castBlisterzoa(driver)
        val handBefore = driver.getHandSize(driver.player1)
        doomBlade(driver, zoa)
        driver.state.getZone(ZoneKey(driver.player1, Zone.GRAVEYARD)).contains(zoa) shouldBe true
        // +1 Doom Blade put in hand then cast (net 0), +1 drawn
        driver.getHandSize(driver.player1) shouldBe handBefore + 1
    }

    test("dies with three oil counters: draws three cards") {
        val driver = newDriver()
        val zoa = castBlisterzoa(driver)
        driver.addComponent(zoa, CountersComponent(mapOf(CounterType.OIL to 3)))
        val handBefore = driver.getHandSize(driver.player1)
        doomBlade(driver, zoa)
        driver.getHandSize(driver.player1) shouldBe handBefore + 3
    }

    test("dies with no oil counters: draws nothing") {
        val driver = newDriver()
        val zoa = driver.putCreatureOnBattlefield(driver.player1, "Watchful Blisterzoa")
        oil(driver, zoa) shouldBe 0
        val handBefore = driver.getHandSize(driver.player1)
        doomBlade(driver, zoa)
        driver.getHandSize(driver.player1) shouldBe handBefore
    }
})
