package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.HorridShadowspinner
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Horrid Shadowspinner (MH3 #188).
 *
 * Oracle: "Lifelink / Whenever this creature attacks, you may draw cards equal to its power.
 * If you do, discard that many cards."
 */
class HorridShadowspinnerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HorridShadowspinner)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("accepting draws cards equal to its power, then discards that many") {
        val driver = newDriver()
        val me = driver.player1
        val spinner = driver.putCreatureOnBattlefield(me, "Horrid Shadowspinner")
        driver.removeSummoningSickness(spinner)

        val handBefore = driver.getHandSize(me)
        val libraryBefore = driver.state.getLibrary(me).size
        val graveyardBefore = driver.getGraveyard(me).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(spinner), driver.player2)
        driver.bothPass()

        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(me, true)

        // Drew two (power 2), now must discard two.
        driver.state.getLibrary(me).size shouldBe libraryBefore - 2
        driver.getHandSize(me) shouldBe handBefore + 2
        driver.submitCardSelection(me, driver.getHand(me).take(2))

        driver.getHandSize(me) shouldBe handBefore
        driver.getGraveyard(me).size shouldBe graveyardBefore + 2
    }

    test("the count follows its current power, not its printed power") {
        val driver = newDriver()
        val me = driver.player1
        val spinner = driver.putCreatureOnBattlefield(me, "Horrid Shadowspinner")
        driver.removeSummoningSickness(spinner)
        driver.replaceState(
            driver.state.updateEntity(spinner) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
        )

        val handBefore = driver.getHandSize(me)
        val libraryBefore = driver.state.getLibrary(me).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(spinner), driver.player2)
        driver.bothPass()
        driver.submitYesNo(me, true)

        driver.state.getLibrary(me).size shouldBe libraryBefore - 3
        driver.submitCardSelection(me, driver.getHand(me).take(3))
        driver.getHandSize(me) shouldBe handBefore
    }

    test("declining draws and discards nothing") {
        val driver = newDriver()
        val me = driver.player1
        val spinner = driver.putCreatureOnBattlefield(me, "Horrid Shadowspinner")
        driver.removeSummoningSickness(spinner)

        val handBefore = driver.getHandSize(me)
        val libraryBefore = driver.state.getLibrary(me).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(spinner), driver.player2)
        driver.bothPass()

        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(me, false)

        driver.getHandSize(me) shouldBe handBefore
        driver.state.getLibrary(me).size shouldBe libraryBefore
    }
})
