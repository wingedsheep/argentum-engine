package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bounding Felidar (OTJ #5) — {5}{W} Cat Beast Mount, 4/7, Saddle 2.
 *
 *   "Whenever this creature attacks while saddled, put a +1/+1 counter on each other creature you
 *    control. You gain 1 life for each of those creatures."
 *
 * Verifies the saddle-gated attack trigger: each OTHER creature you control gets a +1/+1 counter
 * (the Felidar itself does not), and the controller gains 1 life per other creature.
 */
class BoundingFelidarScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attacking while saddled pumps each other creature and gains life per creature") {
        val driver = createDriver()
        val player = driver.player1

        val felidar = driver.putCreatureOnBattlefield(player, "Bounding Felidar")
        val bear1 = driver.putCreatureOnBattlefield(player, "Grizzly Bears") // 2/2, power 2 → saddles
        val bear2 = driver.putCreatureOnBattlefield(player, "Grizzly Bears") // 2/2
        driver.removeSummoningSickness(felidar)

        val lifeBefore = driver.getLifeTotal(player)
        driver.state.projectedState.getPower(bear1) shouldBe 2
        driver.state.projectedState.getPower(bear2) shouldBe 2

        // Saddle with bear1 (power 2 pays Saddle 2). bear2 stays untapped to receive a counter.
        driver.submitSuccess(SaddleMount(player, felidar, listOf(bear1)))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(felidar), driver.player2)
        driver.bothPass() // resolve the attack trigger

        // Both bears get a +1/+1 counter (now 3/3); the Felidar itself does not.
        driver.state.projectedState.getPower(bear1) shouldBe 3
        driver.state.projectedState.getPower(bear2) shouldBe 3
        driver.state.projectedState.getPower(felidar) shouldBe 4

        // Gain 1 life for each of the 2 other creatures you control.
        driver.getLifeTotal(player) shouldBe lifeBefore + 2
    }

    test("attacking while NOT saddled does nothing") {
        val driver = createDriver()
        val player = driver.player1

        val felidar = driver.putCreatureOnBattlefield(player, "Bounding Felidar")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(felidar)

        val lifeBefore = driver.getLifeTotal(player)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(felidar), driver.player2)
        driver.bothPass()

        driver.state.projectedState.getPower(bear) shouldBe 2
        driver.getLifeTotal(player) shouldBe lifeBefore
    }
})
