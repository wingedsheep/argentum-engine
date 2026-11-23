package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.ObzedatGhostCouncil
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Obzedat, Ghost Council — the "you may exile it at your end step; it returns at
 * the beginning of your next upkeep with haste" loop.
 *
 * Accepting exiles Obzedat through the opponent's whole turn (upkeep included), then the delayed
 * trigger returns it on *your* upkeep with permanent haste, so it can attack the turn it comes back.
 * Declining leaves it on the battlefield and nothing is scheduled.
 */
class ObzedatGhostCouncilScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ObzedatGhostCouncil)
        driver.initMirrorMatch(Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("exiled at your end step, returns at your next upkeep with haste and can attack") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val obzedat = driver.putCreatureOnBattlefield(me, "Obzedat, Ghost Council")

        driver.passPriorityUntil(Step.END)
        driver.bothPass() // end-step trigger resolves -> may prompt
        driver.submitYesNo(me, true)

        driver.getExile(me) shouldContain obzedat
        driver.getPermanents(me) shouldNotContain obzedat

        withClue("still exiled through the opponent's upkeep") {
            driver.passPriorityUntil(Step.DRAW, opponent)
            driver.getExile(me) shouldContain obzedat
        }

        driver.passPriorityUntil(Step.UPKEEP, me)
        driver.bothPass() // delayed upkeep trigger resolves

        driver.getPermanents(me) shouldContain obzedat
        driver.getExile(me) shouldNotContain obzedat
        driver.state.projectedState.hasKeyword(obzedat, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        withClue("haste lets it attack the turn it returns") {
            driver.declareAttackers(me, listOf(obzedat), opponent).outcome shouldBe Outcome.Done
        }
    }

    test("declining keeps Obzedat on the battlefield without haste") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val obzedat = driver.putCreatureOnBattlefield(me, "Obzedat, Ghost Council")

        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.submitYesNo(me, false)

        driver.getPermanents(me) shouldContain obzedat
        driver.getExile(me) shouldNotContain obzedat
        driver.state.projectedState.hasKeyword(obzedat, Keyword.HASTE) shouldBe false
    }
})
