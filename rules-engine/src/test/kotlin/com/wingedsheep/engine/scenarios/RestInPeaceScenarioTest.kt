package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rtr.cards.RestInPeace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rest in Peace (Return to Ravnica #18):
 *   "When this enchantment enters, exile all graveyards.
 *    If a card or token would be put into a graveyard from anywhere, exile it instead."
 *
 * The ETB clause is the Gather -> Move pipeline over every player's graveyard; the static clause
 * is the reusable [com.wingedsheep.sdk.scripting.RedirectZoneChange] graveyard -> exile replacement
 * with an unrestricted "to graveyard from anywhere" event pattern. These tests prove (a) the ETB
 * sweeps both players' graveyards into exile and (b) the replacement diverts a dying creature and a
 * resolved instant to exile rather than the graveyard.
 */
class RestInPeaceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(RestInPeace))
        return driver
    }

    fun startTurn(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver.activePlayer!!
    }

    test("ETB exiles every player's graveyard") {
        val driver = createDriver()
        val you = startTurn(driver)
        val opp = driver.getOpponent(you)

        val yourCard = driver.putCardInGraveyard(you, "Lightning Bolt")
        val oppCard = driver.putCardInGraveyard(opp, "Counterspell")

        val rip = driver.putCardInHand(you, "Rest in Peace")
        driver.giveMana(you, Color.WHITE, 2)
        driver.castSpell(you, rip).isSuccess shouldBe true
        driver.bothPass() // Rest in Peace resolves and enters
        driver.bothPass() // the ETB "exile all graveyards" trigger resolves

        driver.getGraveyard(you) shouldNotContain yourCard
        driver.getGraveyard(opp) shouldNotContain oppCard
        driver.getExile(you) shouldContain yourCard
        driver.getExile(opp) shouldContain oppCard
    }

    test("a creature that would die is exiled instead") {
        val driver = createDriver()
        val you = startTurn(driver)
        val opp = driver.getOpponent(you)

        driver.putPermanentOnBattlefield(you, "Rest in Peace")
        val victim = driver.putCreatureOnBattlefield(opp, "Savannah Lions") // 1/1

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(victim)).isSuccess shouldBe true
        driver.bothPass() // Bolt resolves, deals 3 to the 1/1 — it would die

        driver.getGraveyard(opp) shouldNotContain victim
        driver.getExile(opp) shouldContain victim
    }

    test("a resolved instant is exiled instead of going to its owner's graveyard") {
        val driver = createDriver()
        val you = startTurn(driver)

        driver.putPermanentOnBattlefield(you, "Rest in Peace")
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(you)).isSuccess shouldBe true
        driver.bothPass() // Bolt resolves and would head to the graveyard

        driver.getGraveyard(you) shouldNotContain bolt
        driver.getExile(you) shouldContain bolt
    }
})
