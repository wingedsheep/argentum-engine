package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.HobgoblinCaptain
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hobgoblin Captain (AFR #148) — "Pack tactics — Whenever this creature attacks, if you attacked
 * with creatures with total power 6 or greater this combat, this creature gains first strike until
 * end of turn."
 */
class HobgoblinCaptainScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HobgoblinCaptain)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard < 20) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    test("gains first strike when attacking alongside enough power") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val captain = driver.putCreatureOnBattlefield(me, "Hobgoblin Captain") // 3
        val centaur = driver.putCreatureOnBattlefield(me, "Centaur Courser")   // 3
        listOf(captain, centaur).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(captain, centaur), opp)
        driver.drainStack()

        driver.state.projectedState.hasKeyword(captain, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("doesn't gain first strike when attacking alone") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val captain = driver.putCreatureOnBattlefield(me, "Hobgoblin Captain")
        driver.removeSummoningSickness(captain)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(captain), opp)
        driver.drainStack()

        driver.state.projectedState.hasKeyword(captain, Keyword.FIRST_STRIKE) shouldBe false
    }
})
