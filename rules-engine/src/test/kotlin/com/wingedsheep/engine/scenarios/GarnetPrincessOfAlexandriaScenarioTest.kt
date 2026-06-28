package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.GarnetPrincessOfAlexandria
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Garnet, Princess of Alexandria — {G}{W} Legendary Creature — Human Noble Cleric 2/2, Lifelink.
 *   Whenever Garnet attacks, you may remove a lore counter from each of any number of Sagas you
 *   control. Put a +1/+1 counter on Garnet for each lore counter removed this way.
 *
 * Verifies the composed pipeline: gather your Sagas with a lore counter → choose any number →
 * remove one lore counter from each chosen Saga → put that many +1/+1 counters on Garnet. Both the
 * "two chosen" (count scales) and "decline / none chosen" (no counters) paths are covered.
 */
class GarnetPrincessOfAlexandriaScenarioTest : FunSpec({

    // A harmless 4-chapter Saga used only to provide controllable lore counters. 4 chapters keeps
    // it from being sacrificed while it carries fewer than 4 lore counters (CR 714.4).
    val TestLoreSaga = card("Test Lore Saga") {
        manaCost = "{2}"
        typeLine = "Enchantment — Saga"
        oracleText = "I, II, III, IV — You gain 1 life."
        sagaChapter(1) { effect = Effects.GainLife(1) }
        sagaChapter(2) { effect = Effects.GainLife(1) }
        sagaChapter(3) { effect = Effects.GainLife(1) }
        sagaChapter(4) { effect = Effects.GainLife(1) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GarnetPrincessOfAlexandria, TestLoreSaga))
        return driver
    }

    fun setLore(driver: GameTestDriver, entityId: EntityId, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withCounters(CounterType.LORE, count))
        }
        driver.replaceState(newState)
    }

    fun loreCount(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

    fun plusOneCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun drainStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty() && driver.pendingDecision == null) {
            driver.bothPass()
        }
    }

    test("attacking with Garnet: choosing two Sagas removes a lore counter from each and adds two +1/+1 counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val garnet = driver.putCreatureOnBattlefield(active, "Garnet, Princess of Alexandria")
        driver.removeSummoningSickness(garnet)

        val saga1 = driver.putPermanentOnBattlefield(active, "Test Lore Saga")
        val saga2 = driver.putPermanentOnBattlefield(active, "Test Lore Saga")
        drainStack(driver)
        setLore(driver, saga1, 2)
        setLore(driver, saga2, 2)

        plusOneCounters(driver, garnet) shouldBe 0

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(garnet), defendingPlayer = opponent).error shouldBe null

        // Let the attack trigger go on the stack and resolve until it pauses for the selection.
        var guard = 0
        while (guard++ < 20 && driver.pendingDecision == null) driver.bothPass()
        driver.submitCardSelection(active, listOf(saga1, saga2)).error shouldBe null
        drainStack(driver)

        // One lore counter removed from each chosen Saga, two +1/+1 counters on Garnet.
        loreCount(driver, saga1) shouldBe 1
        loreCount(driver, saga2) shouldBe 1
        plusOneCounters(driver, garnet) shouldBe 2
    }

    test("attacking with Garnet: choosing no Sagas leaves lore counters intact and adds no +1/+1 counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val garnet = driver.putCreatureOnBattlefield(active, "Garnet, Princess of Alexandria")
        driver.removeSummoningSickness(garnet)

        val saga1 = driver.putPermanentOnBattlefield(active, "Test Lore Saga")
        drainStack(driver)
        setLore(driver, saga1, 2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(garnet), defendingPlayer = opponent).error shouldBe null

        var guard = 0
        while (guard++ < 20 && driver.pendingDecision == null) driver.bothPass()
        // "you may … any number" — decline by choosing zero Sagas.
        driver.submitCardSelection(active, emptyList()).error shouldBe null
        drainStack(driver)

        loreCount(driver, saga1) shouldBe 2
        plusOneCounters(driver, garnet) shouldBe 0
    }
})
