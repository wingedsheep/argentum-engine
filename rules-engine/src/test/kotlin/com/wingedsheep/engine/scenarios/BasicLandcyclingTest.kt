package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for the BasicLandcycling keyword via Stratosoarer from Lorwyn Eclipsed.
 *
 *   Stratosoarer — {4}{U} Creature — Elemental 3/5
 *   Flying
 *   When this creature enters, target creature gains flying until end of turn.
 *   Basic landcycling {1}{U} ({1}{U}, Discard this card: Search your library for a basic
 *   land card, reveal it, put it into your hand, then shuffle.)
 */
class BasicLandcyclingTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("basic landcycling pays cost, discards, and searches for a basic land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stratosoarer = driver.putCardInHand(activePlayer, "Stratosoarer")
        val island = driver.putCardOnTopOfLibrary(activePlayer, "Island")
        driver.giveMana(activePlayer, com.wingedsheep.sdk.core.Color.BLUE, 1)
        driver.giveColorlessMana(activePlayer, 1)

        val result = driver.submit(TypecycleCard(playerId = activePlayer, cardId = stratosoarer))
        (result.isSuccess || result.isPaused).shouldBeTrue()

        // Stratosoarer was discarded into the graveyard.
        driver.getGraveyardCardNames(activePlayer) shouldContain "Stratosoarer"
        driver.findCardInHand(activePlayer, "Stratosoarer") shouldBe null

        // Search library decision presenting the Island (basic land) as an option.
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain island

        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(decision.id, listOf(island))
        )

        driver.findCardInHand(activePlayer, "Island") shouldBe island
        driver.state.getLibrary(activePlayer) shouldNotContain island
    }

    test("basic landcycling fails without the mana to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stratosoarer = driver.putCardInHand(activePlayer, "Stratosoarer")

        val result = driver.submit(TypecycleCard(playerId = activePlayer, cardId = stratosoarer))
        result.isSuccess shouldBe false
        driver.findCardInHand(activePlayer, "Stratosoarer") shouldBe stratosoarer
    }
})
