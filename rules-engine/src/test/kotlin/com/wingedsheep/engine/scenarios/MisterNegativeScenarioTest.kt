package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mister Negative (SPM) — "you may exchange life totals with target opponent. If you lost life this
 * way, draw that many cards." Pins the new `Effects.ExchangeLifeTotals(drawEqualToLifeLost = true)`
 * (CR 701.12c simultaneous swap + draw = life lost).
 */
class MisterNegativeScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("higher-life controller swaps totals and draws cards equal to life lost") {
        val (driver, you, opponent) = newGame()
        driver.setLifeTotal(you, 12)
        driver.setLifeTotal(opponent, 5)

        driver.giveMana(you, Color.WHITE, 1)
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 5)
        val mn = driver.putCardInHand(you, "Mister Negative")
        driver.castSpell(you, mn)
        resolveStack(driver) // MN resolves; ETB auto-targets the sole opponent, then pauses on "may"

        val handBefore = driver.getHandSize(you)
        driver.pendingDecision as YesNoDecision
        driver.submitYesNo(you, true) // yes, exchange
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe 5        // was 12, now the opponent's former 5
        driver.getLifeTotal(opponent) shouldBe 12  // was 5, now the controller's former 12
        driver.getHandSize(you) shouldBe handBefore + 7 // lost 7 life → drew 7
    }
})
