package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.nph.cards.TorporOrb
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
 * Scenario tests for Torpor Orb (New Phyrexia #162):
 *   "Creatures entering don't cause abilities to trigger."
 *
 * Backed by the reusable [com.wingedsheep.sdk.scripting.SuppressEntersTriggers] static (filter =
 * creatures). The probe creature is Elvish Visionary ("When this creature enters, draw a card");
 * a known card is seeded on top of the controller's library, so it reaches the hand iff the ETB
 * draw fires. Edge cases: the lock lifts when Torpor Orb leaves, and a *noncreature* permanent's
 * ETB trigger (Rest in Peace's "exile all graveyards") still fires — the filter excludes it.
 */
class TorporOrbScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(TorporOrb, RestInPeace))
        return driver
    }

    fun startTurn(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver.activePlayer!!
    }

    test("a creature's enters-the-battlefield draw trigger is suppressed while Torpor Orb is in play") {
        val driver = createDriver()
        val you = startTurn(driver)

        driver.putPermanentOnBattlefield(you, "Torpor Orb")
        val seeded = driver.putCardOnTopOfLibrary(you, "Lightning Bolt")
        val visionary = driver.putCardInHand(you, "Elvish Visionary") // {1}{G}, ETB draw a card
        driver.giveMana(you, Color.GREEN, 2)

        driver.castSpell(you, visionary).isSuccess shouldBe true
        driver.bothPass() // Elvish Visionary resolves and enters
        driver.bothPass() // (would-be) ETB draw trigger resolves — suppressed by Torpor Orb

        driver.getHand(you) shouldNotContain seeded
    }

    test("the same creature draws once Torpor Orb has left the battlefield") {
        val driver = createDriver()
        val you = startTurn(driver)

        val seeded = driver.putCardOnTopOfLibrary(you, "Lightning Bolt")
        val visionary = driver.putCardInHand(you, "Elvish Visionary")
        driver.giveMana(you, Color.GREEN, 2)

        driver.castSpell(you, visionary).isSuccess shouldBe true
        driver.bothPass() // resolves and enters
        driver.bothPass() // ETB draw trigger resolves — no Torpor Orb, so it fires

        driver.getHand(you) shouldContain seeded
    }

    test("a noncreature permanent's enters trigger is NOT suppressed") {
        val driver = createDriver()
        val you = startTurn(driver)
        val opp = driver.getOpponent(you)

        driver.putPermanentOnBattlefield(you, "Torpor Orb")
        val ghost = driver.putCardInGraveyard(opp, "Counterspell")

        val rip = driver.putCardInHand(you, "Rest in Peace") // Enchantment; ETB exiles all graveyards
        driver.giveMana(you, Color.WHITE, 2)
        driver.castSpell(you, rip).isSuccess shouldBe true
        driver.bothPass() // Rest in Peace resolves and enters
        driver.bothPass() // its ETB "exile all graveyards" trigger resolves (a noncreature enters)

        // The enchantment's ETB still fired — the graveyard was exiled.
        driver.getGraveyard(opp) shouldNotContain ghost
        driver.getExile(opp) shouldContain ghost
    }
})
