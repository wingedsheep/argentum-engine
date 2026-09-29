package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Loyal Sentry (S99).
 *
 * {W} Creature — Human Soldier 1/1
 * "When this creature blocks a creature, destroy that creature and this creature."
 */
class LoyalSentryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("blocking destroys both the Sentry and the blocked creature") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val giant = driver.putCreatureOnBattlefield(attacker, "Hill Giant")
        driver.removeSummoningSickness(giant)
        val sentry = driver.putCreatureOnBattlefield(defender, "Loyal Sentry")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(giant), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(sentry to listOf(giant)))

        driver.stackSize shouldBe 1
        driver.state.step shouldBe Step.DECLARE_BLOCKERS
        driver.bothPass()

        driver.getGraveyardCardNames(attacker) shouldContain "Hill Giant"
        driver.getGraveyardCardNames(defender) shouldContain "Loyal Sentry"
        driver.state.step shouldBe Step.DECLARE_BLOCKERS
        driver.getLifeTotal(defender) shouldBe 20
    }

    test("the blocked creature is destroyed even if the sentry leaves before resolution") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2
        val giant = driver.putCreatureOnBattlefield(attacker, "Hill Giant")
        driver.removeSummoningSickness(giant)
        val sentry = driver.putCreatureOnBattlefield(defender, "Loyal Sentry")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(giant), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(sentry to listOf(giant)))
        driver.stackSize shouldBe 1

        driver.giveMana(attacker, Color.RED, 1)
        val bolt = driver.putCardInHand(attacker, "Lightning Bolt")
        if (driver.state.priorityPlayerId != attacker) {
            driver.passPriority(defender).error shouldBe null
        }
        driver.castSpell(attacker, bolt, listOf(sentry)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.getGraveyardCardNames(defender) shouldContain "Loyal Sentry"
        driver.stackSize shouldBe 1

        driver.bothPass().error shouldBe null
        driver.getGraveyardCardNames(attacker) shouldContain "Hill Giant"
        driver.getLifeTotal(defender) shouldBe 20
    }
})
