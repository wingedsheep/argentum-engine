package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Rambling Possum (OTJ).
 *
 * Oracle: "Whenever this creature attacks while saddled, it gets +1/+2 until end of turn. Then you
 * may return any number of creatures that saddled it this turn to their owner's hand. Saddle 1"
 *
 * Covered:
 *  - Attacking while saddled buffs +1/+2 and offers the "return any number of saddlers" choice.
 *  - The player may return a chosen subset of the saddlers (the rest stay on the battlefield).
 *  - The player may decline (return zero) — saddlers stay put.
 *  - Attacking while NOT saddled does not trigger.
 */
class RamblingPossumScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0

    test("attacking while saddled buffs +1/+2 and returns chosen saddlers to hand") {
        val driver = newDriver()
        val p1 = driver.player1
        val possum = driver.putCreatureOnBattlefield(p1, "Rambling Possum")
        val saddlerA = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val saddlerB = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(possum)

        // Saddle 1 needs total power >= 1; one Grizzly Bears suffices, but saddle with both so the
        // "any number" choice has more than one candidate.
        driver.submitSuccess(SaddleMount(p1, possum, listOf(saddlerA, saddlerB)))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(possum), driver.player2)
        driver.bothPass() // resolve the attack trigger up to the return-any-number choice

        // The attack trigger buffs +1/+2, then offers the return-any-number choice over the saddlers.
        driver.power(possum) shouldBe 4   // 3 + 1
        driver.toughness(possum) shouldBe 5 // 3 + 2

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options.toSet() shouldBe setOf(saddlerA, saddlerB)
        // Return only saddlerA.
        driver.submitCardSelection(p1, listOf(saddlerA))
        driver.bothPass()

        driver.getHand(p1).contains(saddlerA) shouldBe true
        // saddlerB was not chosen — still on the battlefield.
        (saddlerB in driver.state.getBattlefield()) shouldBe true
        (saddlerA in driver.state.getBattlefield()) shouldBe false
    }

    test("player may decline and return no saddlers") {
        val driver = newDriver()
        val p1 = driver.player1
        val possum = driver.putCreatureOnBattlefield(p1, "Rambling Possum")
        val saddler = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(possum)

        driver.submitSuccess(SaddleMount(p1, possum, listOf(saddler)))
        driver.bothPass()
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(possum), driver.player2)
        driver.bothPass() // resolve the attack trigger up to the return-any-number choice

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(p1, emptyList()) // decline
        driver.bothPass()

        (saddler in driver.state.getBattlefield()) shouldBe true
        driver.getHand(p1).contains(saddler) shouldBe false
        driver.power(possum) shouldBe 4 // still buffed
    }

    test("attacking while not saddled does not trigger") {
        val driver = newDriver()
        val p1 = driver.player1
        val possum = driver.putCreatureOnBattlefield(p1, "Rambling Possum")
        driver.removeSummoningSickness(possum)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(possum), driver.player2)
        driver.bothPass()

        // No buff, no decision.
        driver.power(possum) shouldBe 3
        driver.toughness(possum) shouldBe 3
    }
})
