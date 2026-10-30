package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome

/**
 * Tests for Delivery Moogle (FIN #15).
 *
 * Delivery Moogle {3}{W} Creature — Moogle 3/2
 * Flying
 * When this creature enters, search your library and/or graveyard for an artifact card
 * with mana value 2 or less, reveal it, and put it into your hand. If you search your
 * library this way, shuffle.
 */
class DeliveryMoogleScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("ETB searches both library and graveyard for a cheap artifact to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A cheap artifact in each of the two searchable zones.
        val ornithopterInLibrary = driver.putCardOnTopOfLibrary(active, "Ornithopter") // MV 0
        val bonesplitterInGraveyard = driver.putCardInGraveyard(active, "Bonesplitter")  // MV 1

        val moogle = driver.putCardInHand(active, "Delivery Moogle")
        driver.giveMana(active, Color.WHITE, 1)
        driver.giveColorlessMana(active, 3)

        driver.castSpell(active, moogle).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature; ETB trigger goes on the stack

        driver.bothPass() // resolve the ETB trigger -> presents the multi-zone search

        // "Your library and/or graveyard": choose which zones to search.
        driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>().let {
            driver.submitDecision(active, OptionChosenResponse(it.id, it.options.indexOf("Search your library and graveyard")))
        }
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        // Both the library artifact and the graveyard artifact are valid choices.
        decision.options shouldContain ornithopterInLibrary
        decision.options shouldContain bonesplitterInGraveyard

        driver.submitDecision(active, CardsSelectedResponse(decision.id, listOf(bonesplitterInGraveyard)))

        driver.findCardInHand(active, "Bonesplitter") shouldBe bonesplitterInGraveyard
    }
})
