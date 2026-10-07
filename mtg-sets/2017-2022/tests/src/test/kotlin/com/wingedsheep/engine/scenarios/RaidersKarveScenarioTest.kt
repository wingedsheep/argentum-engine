package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.RaidersKarve
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RaidersKarveScenarioTest : FunSpec({
    fun attackingWith(topCard: String): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RaidersKarve)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(you, "Raiders' Karve")
        val crew = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.removeSummoningSickness(vehicle)
        val top = driver.putCardOnTopOfLibrary(you, topCard)
        driver.submitSuccess(CrewVehicle(you, vehicle, listOf(crew)))
        driver.bothPass()
        driver.isTapped(crew) shouldBe true
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submitSuccess(DeclareAttackers(you, mapOf(vehicle to driver.getOpponent(you))))
        driver.bothPass()
        return driver to top
    }

    test("crewing and attacking offers the top land and puts it onto the battlefield tapped") {
        val (driver, top) = attackingWith("Forest")
        val you = driver.activePlayer!!
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(top)
        decision.minSelections shouldBe 0
        decision.maxSelections shouldBe 1
        driver.submitCardSelection(you, listOf(top)).error shouldBe null
        driver.findPermanent(you, "Forest") shouldBe top
        driver.isTapped(top) shouldBe true
        driver.state.getLibrary(you).contains(top) shouldBe false
    }

    test("declining the land leaves it on top of the library") {
        val (driver, top) = attackingWith("Forest")
        val you = driver.activePlayer!!
        val before = driver.state.getLibrary(you)
        driver.submitCardSelection(you, emptyList()).error shouldBe null
        driver.state.getLibrary(you) shouldBe before
        driver.state.getLibrary(you).first() shouldBe top
        driver.findPermanent(you, "Forest") shouldBe null
    }

    test("a nonland is shown but cannot be selected and stays on top") {
        val (driver, top) = attackingWith("Grizzly Bears")
        val you = driver.activePlayer!!
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.maxSelections shouldBe 0
        decision.nonSelectableOptions shouldBe listOf(top)
        decision.cardInfo?.containsKey(top) shouldBe true
        driver.submitCardSelection(you, listOf(top)).error.shouldNotBeNull()
        driver.submitCardSelection(you, emptyList()).error shouldBe null
        driver.state.getLibrary(you).first() shouldBe top
    }
})
