package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mat.cards.NahirisResolve
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nahiri's Resolve — {3}{R}{W} Enchantment.
 *
 * At the beginning of your end step, exile any number of nontoken artifacts and/or creatures you
 * control; they return at the beginning of *your* next upkeep (not the opponent's).
 */
class NahirisResolveScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NahirisResolve)
        driver.initMirrorMatch(Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("chosen permanents are exiled at your end step and return at your next upkeep") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Nahiri's Resolve")
        val chosen = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val kept = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val opposing = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.passPriorityUntil(Step.END)
        driver.bothPass() // end-step trigger resolves

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContainExactlyInAnyOrder listOf(chosen, kept)
        driver.submitCardSelection(me, listOf(chosen))

        driver.getExile(me) shouldContain chosen
        driver.getPermanents(me) shouldContain kept
        driver.getPermanents(driver.player2) shouldContain opposing

        // Still exiled through the opponent's turn, including their upkeep.
        driver.passPriorityUntil(Step.DRAW, driver.player2)
        driver.getExile(me) shouldContain chosen

        driver.passPriorityUntil(Step.UPKEEP, me)
        driver.bothPass() // delayed upkeep trigger resolves

        driver.getPermanents(me) shouldContain chosen
        driver.getExile(me) shouldNotContain chosen
    }
})
