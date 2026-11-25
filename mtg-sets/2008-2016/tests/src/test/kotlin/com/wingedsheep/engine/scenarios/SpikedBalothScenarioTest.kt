package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.SpikedBaloth
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spiked Baloth {3}{G} — Creature — Beast 4/2
 *   Trample
 */
class SpikedBalothScenarioTest : FunSpec({

    test("blocked by a 1/1, it tramples the excess 3 damage over to the player") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SpikedBaloth))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val baloth = driver.putCreatureOnBattlefield(me, "Spiked Baloth")
        driver.removeSummoningSickness(baloth)
        val lions = driver.putCreatureOnBattlefield(opp, "Savannah Lions")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(baloth), opp).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opp, mapOf(lions to listOf(baloth))).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.getLifeTotal(opp) shouldBe 17
    }
})
