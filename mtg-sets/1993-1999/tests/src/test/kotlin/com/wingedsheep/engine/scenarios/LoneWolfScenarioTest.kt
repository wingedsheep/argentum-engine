package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Lone Wolf (P02 #131).
 *
 * {2}{G} Creature — Wolf 2/2
 * "You may have this creature assign its combat damage as though it weren't blocked."
 */
class LoneWolfScenarioTest : FunSpec({

    fun blockedAttack(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val wolf = driver.putCreatureOnBattlefield(driver.player1, "Lone Wolf")
        driver.removeSummoningSickness(wolf)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(wolf), driver.player2)
        driver.bothPass()
        driver.declareBlockers(driver.player2, mapOf(blocker to listOf(wolf)))
        driver.bothPass()
        return driver to wolf
    }

    test("blocked, it may assign its damage to the defending player") {
        val (driver, _) = blockedAttack()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(driver.player1, true)

        driver.getLifeTotal(driver.player2) shouldBe 18
        // The blocker takes nothing from the Wolf.
        driver.getGraveyardCardNames(driver.player2).contains("Hill Giant") shouldBe false
    }

    test("declining assigns damage to the blocker as normal") {
        val (driver, _) = blockedAttack()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(driver.player1, false)

        driver.getLifeTotal(driver.player2) shouldBe 20
        // Hill Giant (3/3) kills the 2/2 Wolf.
        driver.getGraveyardCardNames(driver.player1) shouldContain "Lone Wolf"
    }
})
