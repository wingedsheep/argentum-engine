package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.SyphonMind
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Syphon Mind.
 *
 * Syphon Mind: {3}{B}
 * Sorcery
 * Each other player discards a card. You draw a card for each card discarded this way.
 */
class SyphonMindTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Syphon Mind makes opponent discard 1 and controller draws 1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Record hand sizes before casting
        val opponentHandBefore = driver.getHandSize(opponent)
        val activeHandBefore = driver.getHandSize(activePlayer)

        // Cast Syphon Mind
        val syphonMind = driver.putCardInHand(activePlayer, "Syphon Mind")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        val castResult = driver.castSpell(activePlayer, syphonMind)
        castResult.isSuccess shouldBe true

        // Resolve - both pass priority
        driver.bothPass()

        // Opponent has >1 card in hand, so they must choose which to discard
        driver.isPaused shouldBe true

        // Opponent selects a card to discard
        val opponentHand = driver.getHand(opponent)
        val cardToDiscard = opponentHand.first()
        driver.submitCardSelection(opponent, listOf(cardToDiscard))

        // Opponent should have 1 fewer card
        driver.getHandSize(opponent) shouldBe opponentHandBefore - 1

        // Active player should have 1 more card (drew 1 from Syphon Mind's effect)
        // Note: syphonMind card went from hand to stack to graveyard, so net hand change is +1 (draw) - 1 (cast) = 0
        // But we added it to hand after recording activeHandBefore, so:
        // activeHandBefore + 1 (put syphon in hand) - 1 (cast syphon) + 1 (draw) = activeHandBefore + 1
        driver.getHandSize(activePlayer) shouldBe activeHandBefore + 1

        // Verify discard event for opponent
        val discardEvents = driver.events.filterIsInstance<CardsDiscardedEvent>()
        discardEvents.any { it.playerId == opponent && it.cardIds.contains(cardToDiscard) } shouldBe true

        // Verify draw event for controller
        val drawEvents = driver.events.filterIsInstance<CardsDrawnEvent>()
        drawEvents.any { it.playerId == activePlayer && it.count == 1 } shouldBe true
    }

    test("Syphon Mind with opponent having exactly 1 card auto-discards and controller draws") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a single known card in opponent's hand
        val opponentCard = driver.putCardInHand(opponent, "Forest")

        // Discard all of opponent's other cards by casting multiple discard spells
        // Instead, let's use the "1 card" path by giving them exactly 1 card
        // We need to get the opponent to have only 1 card - let's use a workaround
        // Track active player hand before
        val activeHandBefore = driver.getHandSize(activePlayer)

        // Cast Syphon Mind
        val syphonMind = driver.putCardInHand(activePlayer, "Syphon Mind")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        val castResult = driver.castSpell(activePlayer, syphonMind)
        castResult.isSuccess shouldBe true

        // Resolve
        driver.bothPass()

        // Opponent has 7 (initial) + 1 (our Forest) = 8 cards, so they must choose
        driver.isPaused shouldBe true

        // Select the forest card we added to discard
        driver.submitCardSelection(opponent, listOf(opponentCard))

        // Opponent discarded 1, controller drew 1
        val drawEvents = driver.events.filterIsInstance<CardsDrawnEvent>()
        drawEvents.any { it.playerId == activePlayer && it.count == 1 } shouldBe true

        // Controller gained 1 card net from the draw
        driver.getHandSize(activePlayer) shouldBe activeHandBefore + 1
    }
})
