package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Arabella, Abandoned Doll (DSK #208) — {R}{W} 1/3 Legendary Artifact Creature — Toy.
 *
 * "Whenever Arabella attacks, it deals X damage to each opponent and you gain X life, where X is
 *  the number of creatures you control with power 2 or less."
 *
 * Exercises the attack trigger that reuses one resolution-time X (count of your power≤2 creatures)
 * for both the damage to each opponent and the life gain.
 */
class ArabellaAbandonedDollScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attacking deals X damage to the opponent and gains X life, X = power<=2 creatures you control") {
        val driver = newDriver()
        val arabella = driver.putCreatureOnBattlefield(driver.player1, "Arabella, Abandoned Doll")
        driver.removeSummoningSickness(arabella)
        // Two more power<=2 creatures (Grizzly Bears 2/2, Savannah Lions 1/1). Arabella herself is
        // 1/3, also power<=2. So X = 3.
        driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player1, "Savannah Lions")
        // A 3/3 (Hill Giant) is power 3 — must NOT count toward X.
        driver.putCreatureOnBattlefield(driver.player1, "Hill Giant")

        driver.setLifeTotal(driver.player1, 20)
        driver.setLifeTotal(driver.player2, 20)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(arabella), driver.player2)
        driver.bothPass()

        // X = Arabella (1) + Grizzly Bears (2) + Savannah Lions (1) = 3 creatures with power <= 2.
        driver.getLifeTotal(driver.player2) shouldBe 17
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("X is zero when only high-power creatures attack (Arabella is the sole power<=2 creature -> X=1)") {
        val driver = newDriver()
        val arabella = driver.putCreatureOnBattlefield(driver.player1, "Arabella, Abandoned Doll")
        driver.removeSummoningSickness(arabella)
        // Only a 3/3 besides Arabella — it doesn't count. X = 1 (Arabella herself).
        driver.putCreatureOnBattlefield(driver.player1, "Hill Giant")

        driver.setLifeTotal(driver.player1, 20)
        driver.setLifeTotal(driver.player2, 20)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(arabella), driver.player2)
        driver.bothPass()

        driver.getLifeTotal(driver.player2) shouldBe 19
        driver.getLifeTotal(driver.player1) shouldBe 21
    }
})
