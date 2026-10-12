package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.AeronautCavalry
import com.wingedsheep.mtg.sets.definitions.bro.cards.FortifiedBeachhead
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fortified Beachhead — "This land enters tapped unless you revealed a Soldier card this way or
 * you control a Soldier." Pins down both escape hatches and the tapped fallback.
 */
class FortifiedBeachheadScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FortifiedBeachhead, AeronautCavalry))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("controlling a Soldier lets the land enter untapped without a reveal") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Aeronaut Cavalry")
        driver.putCardInHand(player, "Aeronaut Cavalry")
        val beachhead = driver.putCardInHand(player, "Fortified Beachhead")

        driver.playLand(player, beachhead).outcome shouldBe Outcome.Done

        driver.pendingDecision shouldBe null
        driver.isTapped(beachhead) shouldBe false
    }

    test("revealing a Soldier card lets the land enter untapped") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val soldier = driver.putCardInHand(player, "Aeronaut Cavalry")
        val beachhead = driver.putCardInHand(player, "Fortified Beachhead")

        (driver.playLand(player, beachhead).outcome is Outcome.Paused) shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(soldier)

        driver.submitCardSelection(player, listOf(soldier)).outcome shouldBe Outcome.Done

        driver.isTapped(beachhead) shouldBe false
    }

    test("declining the reveal with no Soldier in play taps the land") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardInHand(player, "Aeronaut Cavalry")
        val beachhead = driver.putCardInHand(player, "Fortified Beachhead")

        (driver.playLand(player, beachhead).outcome is Outcome.Paused) shouldBe true
        driver.submitCardSelection(player, emptyList()).outcome shouldBe Outcome.Done

        driver.isTapped(beachhead) shouldBe true
    }

    test("no Soldier anywhere: the land enters tapped") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val beachhead = driver.putCardInHand(player, "Fortified Beachhead")

        driver.playLand(player, beachhead).outcome shouldBe Outcome.Done

        driver.pendingDecision shouldBe null
        driver.isTapped(beachhead) shouldBe true
    }
})
