package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.AinokGuide
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ainok Guide (Fate Reforged #121) — {1}{G} Creature — Dog Scout 1/1.
 *
 * "When this creature enters, choose one — • Put a +1/+1 counter on this creature.
 *  • Search your library for a basic land card, reveal it, then shuffle and put that card on top."
 */
class AinokGuideScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AinokGuide)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Ainok Guide and resolve it until its enters trigger asks for a mode. */
    fun GameTestDriver.castGuideAndChoose(me: EntityId, modePrefix: String): EntityId {
        val guide = putCardInHand(me, "Ainok Guide")
        giveMana(me, Color.GREEN, 2)
        castSpell(me, guide).outcome shouldBe Outcome.Done
        var guard = 0
        while (state.pendingDecision !is ChooseOptionDecision && guard++ < 10) bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.startsWith(modePrefix) }
        check(index >= 0) { "$modePrefix not offered; options=${decision.options}" }
        submitDecision(me, OptionChosenResponse(decision.id, index))
        return guide
    }

    test("first mode puts a +1/+1 counter on it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val guide = driver.castGuideAndChoose(me, "Put a +1/+1 counter")
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 10) {
            driver.bothPass()
        }

        driver.state.projectedState.getPower(guide) shouldBe 2
        driver.state.projectedState.getToughness(guide) shouldBe 2
    }

    test("second mode finds a basic land and puts it on top of the library") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")
        driver.castGuideAndChoose(me, "Search your library")
        var guard = 0
        while (driver.state.pendingDecision !is SelectCardsDecision && guard++ < 10) driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain forest
        decision.options.forEach { driver.getCardName(it) shouldBe "Forest" }
        driver.submitDecision(me, CardsSelectedResponse(decision.id, listOf(forest)))

        driver.state.getZone(me, Zone.LIBRARY).first() shouldBe forest
        driver.getHand(me) shouldNotContain forest
    }
})
