package com.wingedsheep.engine

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * BDD test for the Saga chapter-ability lifecycle:
 *   - lore counter placed on ETB (Rule 702.149b)
 *   - lore counter placed after each controller draw step (Rule 702.149c)
 *   - matching chapter ability triggered by each lore-counter placement
 *   - Saga sacrificed by state-based action after the final chapter ability leaves the stack
 *     (Rule 714.4)
 *
 * The fixture Saga "Test Saga" uses uniquely identifiable life-gain amounts (11 / 22 / 33)
 * so each chapter's resolution can be verified independently via life-total assertions.
 */
class SagaChapterAbilitiesLoreCountersTest : FunSpec({

    val testSaga = card("Test Saga") {
        manaCost = "{2}{G}"
        typeLine = "Enchantment — Saga"
        oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
            "I — You gain 11 life.\n" +
            "II — You gain 22 life.\n" +
            "III — You gain 33 life."

        sagaChapter(1) { effect = GainLifeEffect(11) }
        sagaChapter(2) { effect = GainLifeEffect(22) }
        sagaChapter(3) { effect = GainLifeEffect(33) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(listOf(testSaga) + TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Island" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("saga gains lore counter on ETB, triggers matching chapter ability after each draw step, and is sacrificed after chapter III resolves") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val startingLife = driver.getLifeTotal(controller)

        // Advance to controller's precombat main phase (turn 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Place the Saga on the battlefield — engine ETB rule (Rule 702.149b) must add 1 lore counter
        val sagaId = driver.putPermanentOnBattlefield(controller, "Test Saga")

        // THEN: exactly 1 lore counter immediately after ETB
        val loreAfterEtb = driver.state.getEntity(sagaId)
            ?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0
        loreAfterEtb shouldBe 1

        // Resolve chapter I ability (triggered by lore counter reaching chapter I threshold)
        driver.bothPass()

        // THEN: chapter I resolved — controller gained 11 life
        driver.getLifeTotal(controller) shouldBe startingLife + 11

        // --- Advance to controller's first draw step following ETB (controller's turn 3) ---
        // Skip rest of turn 1 then advance through opponent's full turn 2
        driver.passPriorityUntil(Step.END)
        driver.bothPass()  // cleanup → opponent's turn 2 begins
        driver.passPriorityUntil(Step.END)
        driver.bothPass()  // cleanup → controller's turn 3 begins
        // Engine adds 2nd lore counter after draw step; chapter II trigger is on the stack
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // THEN: 2 lore counters after the first post-ETB draw step
        val loreAfterFirstDraw = driver.state.getEntity(sagaId)
            ?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0
        loreAfterFirstDraw shouldBe 2

        // Resolve chapter II ability
        driver.bothPass()

        // THEN: chapter II resolved — controller gained 22 life
        driver.getLifeTotal(controller) shouldBe startingLife + 11 + 22

        // --- Advance to controller's second draw step following ETB (controller's turn 5) ---
        driver.passPriorityUntil(Step.END)
        driver.bothPass()  // cleanup → opponent's turn 4 begins
        driver.passPriorityUntil(Step.END)
        driver.bothPass()  // cleanup → controller's turn 5 begins
        // Engine adds 3rd lore counter after draw step; chapter III trigger is on the stack
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // THEN: 3 lore counters after the second post-ETB draw step
        val loreAfterSecondDraw = driver.state.getEntity(sagaId)
            ?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0
        loreAfterSecondDraw shouldBe 3

        // Resolve chapter III ability
        driver.bothPass()

        // THEN: chapter III resolved — controller gained 33 life
        driver.getLifeTotal(controller) shouldBe startingLife + 11 + 22 + 33

        // THEN: once chapter III's ability has resolved and the stack is empty, the engine's
        // state-based action (Rule 714.4) sacrifices the Saga — it must now be in the graveyard
        driver.findPermanent(controller, "Test Saga") shouldBe null
        driver.assertInGraveyard(controller, "Test Saga")
    }
})
