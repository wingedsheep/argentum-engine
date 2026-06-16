package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlotCard
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.FblthpLostOnTheRange
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Fblthp, Lost on the Range (OTJ) — {1}{U}{U} 1/1, Ward {2}.
 *
 * "You may look at the top card of your library any time. The top card of your library has plot.
 *  The plot cost is equal to its mana cost. You may plot nonland cards from the top of your library."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.PlotFromTopOfLibrary] static ability:
 * the plot enumerator/handler can plot the top *nonland* card of the library at a cost equal to
 * its mana cost, moving it from library → exile and plotting it (CR 718). A land on top can't be
 * plotted, and the cost is the card's own mana cost.
 */
class FblthpLostOnTheRangeScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FblthpLostOnTheRange)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("plots the top nonland card from the library for its mana cost") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.putPermanentOnBattlefield(p1, "Fblthp, Lost on the Range")

        // Top of library is a Grizzly Bears ({1}{G}) — a nonland card, so plottable.
        val top = driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        driver.giveColorlessMana(p1, 1)
        driver.giveMana(p1, Color.GREEN, 1) // plot cost = its mana cost {1}{G}

        driver.submitSuccess(PlotCard(p1, top))

        // The card left the library and is now exiled + plotted for its owner.
        driver.state.getLibrary(p1).contains(top) shouldBe false
        driver.getExile(p1).contains(top) shouldBe true
        val plotted = driver.state.getEntity(top)?.get<PlottedComponent>()
        plotted shouldNotBe null
        plotted!!.controllerId shouldBe p1
    }

    test("a land on top of the library cannot be plotted") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.putPermanentOnBattlefield(p1, "Fblthp, Lost on the Range")

        val topLand = driver.putCardOnTopOfLibrary(p1, "Island")
        driver.giveColorlessMana(p1, 3)

        // Plotting a land from the top is illegal (the grant is filtered to nonland).
        driver.submit(PlotCard(p1, topLand)).isSuccess shouldBe false
        driver.state.getLibrary(p1).contains(topLand) shouldBe true
    }

    test("without Fblthp, the top card of the library is not plottable") {
        val driver = newDriver()
        val p1 = driver.player1

        val top = driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        driver.giveColorlessMana(p1, 1)
        driver.giveMana(p1, Color.GREEN, 1)

        driver.submit(PlotCard(p1, top)).isSuccess shouldBe false
        driver.state.getLibrary(p1).contains(top) shouldBe true
    }
})
