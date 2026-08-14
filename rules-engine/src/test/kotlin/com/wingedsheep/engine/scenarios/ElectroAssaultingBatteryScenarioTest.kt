package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Electro, Assaulting Battery (SPM) — "You don't lose unspent red mana as steps and phases end."
 * Pins the new `RetainUnspentColoredMana(Color.RED)` static wired into
 * `CleanupPhaseManager.emptyManaPools`.
 */
class ElectroAssaultingBatteryScenarioTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    test("red mana survives a step/phase boundary; other colors still empty") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, "Electro, Assaulting Battery")
        driver.giveMana(you, Color.RED, 2)
        driver.giveMana(you, Color.GREEN, 1)

        // Cross into combat — the precombat-main → combat boundary empties mana pools.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val pool = driver.state.getEntity(you)?.get<ManaPoolComponent>()
        pool?.red shouldBe 2   // retained by Electro
        pool?.green shouldBe 0 // still emptied
    }

    test("without Electro, red mana empties at the boundary too") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, "Centaur Courser") // so combat reaches declare-attackers
        driver.giveMana(you, Color.RED, 2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        (driver.state.getEntity(you)?.get<ManaPoolComponent>()?.red ?: 0) shouldBe 0
    }
})
