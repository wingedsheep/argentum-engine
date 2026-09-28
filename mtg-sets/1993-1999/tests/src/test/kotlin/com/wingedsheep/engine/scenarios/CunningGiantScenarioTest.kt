package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Cunning Giant (P02 #93).
 *
 * {5}{R} Creature — Giant 4/4
 * "If this creature is unblocked, you may have it assign its combat damage to a creature defending
 * player controls."
 */
class CunningGiantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun attackUnblocked(driver: GameTestDriver, giant: com.wingedsheep.sdk.model.EntityId) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(giant), driver.player2)
        driver.bothPass()
        driver.declareNoBlockers(driver.player2)
        driver.bothPass()
    }

    test("unblocked, it may assign all its damage to a creature the defending player controls") {
        val driver = createDriver()
        val giant = driver.putCreatureOnBattlefield(driver.player1, "Cunning Giant")
        driver.removeSummoningSickness(giant)
        val hillGiant = driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        attackUnblocked(driver, giant)

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe driver.player1
        decision.minSelections shouldBe 0
        decision.maxSelections shouldBe 1
        // Only the defending player's creatures — not the attacker's own Grizzly Bears.
        decision.options shouldContainExactlyInAnyOrder listOf(hillGiant, bears)

        driver.submitCardSelection(driver.player1, listOf(hillGiant))

        driver.getGraveyardCardNames(driver.player2) shouldContain "Hill Giant"
        driver.getLifeTotal(driver.player2) shouldBe 20
        // Assigned to a creature that isn't blocking it, so nothing deals damage back.
        driver.findPermanent(driver.player1, "Cunning Giant") shouldBe giant
    }

    test("declining assigns the damage to the defending player as normal") {
        val driver = createDriver()
        val giant = driver.putCreatureOnBattlefield(driver.player1, "Cunning Giant")
        driver.removeSummoningSickness(giant)
        driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")

        attackUnblocked(driver, giant)
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(driver.player1, emptyList())

        driver.getLifeTotal(driver.player2) shouldBe 16
        driver.getGraveyardCardNames(driver.player2).contains("Hill Giant") shouldBe false
    }

    test("no question when the defending player controls no creatures") {
        val driver = createDriver()
        val giant = driver.putCreatureOnBattlefield(driver.player1, "Cunning Giant")
        driver.removeSummoningSickness(giant)

        attackUnblocked(driver, giant)

        driver.pendingDecision.shouldBeNull()
        driver.getLifeTotal(driver.player2) shouldBe 16
    }

    test("a blocked Cunning Giant deals damage to its blocker, with no question") {
        val driver = createDriver()
        val giant = driver.putCreatureOnBattlefield(driver.player1, "Cunning Giant")
        driver.removeSummoningSickness(giant)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(giant), driver.player2)
        driver.bothPass()
        driver.declareBlockers(driver.player2, mapOf(bears to listOf(giant)))
        driver.bothPass()

        driver.pendingDecision.shouldBeNull()
        driver.getGraveyardCardNames(driver.player2) shouldContain "Grizzly Bears"
        driver.getGraveyardCardNames(driver.player2).contains("Hill Giant") shouldBe false
        driver.getLifeTotal(driver.player2) shouldBe 20
    }
})
