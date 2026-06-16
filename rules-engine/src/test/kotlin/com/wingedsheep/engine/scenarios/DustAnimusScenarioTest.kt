package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlotCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dust Animus (OTJ).
 *
 * Oracle: "Flying. If you control five or more untapped lands, this creature enters with two +1/+1
 * counters and a lifelink counter on it. Plot {1}{W}"
 *
 * The conditional enters-with-counters clause is two EntersWithCounters replacement effects gated
 * on "five or more untapped lands you control", evaluated as the creature enters.
 *
 * Covered:
 *  - With 5+ untapped lands: enters as a 4/5 (two +1/+1 counters) with lifelink (lifelink counter).
 *  - With fewer than 5 untapped lands: enters as a vanilla 2/3 flier, no counters.
 *  - Tapped lands don't count toward the five.
 *  - It can be plotted (Plot {1}{W}) from hand.
 */
class DustAnimusScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0

    test("enters with two +1/+1 and a lifelink counter when controlling five untapped lands") {
        val driver = newDriver()
        val p1 = driver.player1
        repeat(5) { driver.putLandOnBattlefield(p1, "Plains") }

        val animus = driver.putCardInHand(p1, "Dust Animus")
        driver.giveMana(p1, Color.WHITE, 2)
        driver.castSpell(p1, animus).isSuccess shouldBe true
        driver.bothPass() // resolve → enters the battlefield

        driver.power(animus) shouldBe 4   // 2 + two +1/+1 counters
        driver.toughness(animus) shouldBe 5 // 3 + two +1/+1 counters
        driver.state.projectedState.hasKeyword(animus, Keyword.FLYING) shouldBe true
        driver.state.projectedState.hasKeyword(animus, Keyword.LIFELINK) shouldBe true
    }

    test("enters as a vanilla 2/3 flier with fewer than five untapped lands") {
        val driver = newDriver()
        val p1 = driver.player1
        repeat(4) { driver.putLandOnBattlefield(p1, "Plains") }

        val animus = driver.putCardInHand(p1, "Dust Animus")
        driver.giveMana(p1, Color.WHITE, 2)
        driver.castSpell(p1, animus).isSuccess shouldBe true
        driver.bothPass()

        driver.power(animus) shouldBe 2
        driver.toughness(animus) shouldBe 3
        driver.state.projectedState.hasKeyword(animus, Keyword.FLYING) shouldBe true
        driver.state.projectedState.hasKeyword(animus, Keyword.LIFELINK) shouldBe false
    }

    test("tapped lands do not count toward the five") {
        val driver = newDriver()
        val p1 = driver.player1
        // 5 lands, but tap one so only 4 are untapped.
        val lands = (0 until 5).map { driver.putLandOnBattlefield(p1, "Plains") }
        driver.tapPermanent(lands.first())

        val animus = driver.putCardInHand(p1, "Dust Animus")
        driver.giveMana(p1, Color.WHITE, 2)
        driver.castSpell(p1, animus).isSuccess shouldBe true
        driver.bothPass()

        // Only 4 untapped lands → no counters.
        driver.power(animus) shouldBe 2
        driver.toughness(animus) shouldBe 3
    }

    test("can be plotted from hand for its plot cost") {
        val driver = newDriver()
        val p1 = driver.player1
        val animus = driver.putCardInHand(p1, "Dust Animus")
        driver.giveMana(p1, Color.WHITE, 2) // plot cost {1}{W}

        driver.submitSuccess(PlotCard(p1, animus))

        driver.getExile(p1).contains(animus) shouldBe true
        driver.getHand(p1).contains(animus) shouldBe false
    }
})
