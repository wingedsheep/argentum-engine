package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize

/**
 * Tests for hand size discard during cleanup step.
 *
 * Per MTG rules, at the beginning of the cleanup step, if the active player
 * has more than seven cards in hand, they must discard down to seven.
 */
class HandSizeDiscardTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("player with 7 or fewer cards does not need to discard") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        // Starting hand is 7 cards - should not need to discard
        driver.getHandSize(activePlayer) shouldBe 7

        // Pass through the entire turn to cleanup
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // Turn should advance without requiring discard (first player doesn't draw)
        // No pending decision should exist
        driver.pendingDecision shouldBe null
    }

    test("player with more than 7 cards must discard to 7 during cleanup") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        // Add extra cards to hand (starting with 7, add 3 more = 10 total)
        driver.putCardInHand(activePlayer, "Grizzly Bears")
        driver.putCardInHand(activePlayer, "Grizzly Bears")
        driver.putCardInHand(activePlayer, "Grizzly Bears")

        driver.getHandSize(activePlayer) shouldBe 10

        // Pass through the entire turn to cleanup
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // Should now be paused waiting for discard decision
        driver.isPaused shouldBe true
        driver.pendingDecision shouldNotBe null
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        val decision = driver.pendingDecision as SelectCardsDecision
        decision.playerId shouldBe activePlayer
        decision.minSelections shouldBe 3  // 10 - 7 = 3 cards to discard
        decision.maxSelections shouldBe 3
        decision.prompt shouldBe "Discard down to 7 cards (choose 3 to discard)"
    }

    test("selecting cards to discard moves them to graveyard") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        // Add 2 extra cards to hand (7 + 2 = 9 total)
        val extra1 = driver.putCardInHand(activePlayer, "Grizzly Bears")
        val extra2 = driver.putCardInHand(activePlayer, "Grizzly Bears")

        driver.getHandSize(activePlayer) shouldBe 9

        // Pass through to cleanup
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // Should be waiting for discard
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision as SelectCardsDecision
        decision.minSelections shouldBe 2

        // Select the two extra cards to discard
        driver.submitCardSelection(activePlayer, listOf(extra1, extra2))

        // Should no longer be paused
        driver.isPaused shouldBe false

        // Hand should now be 7
        driver.getHandSize(activePlayer) shouldBe 7

        // Cards should be in graveyard
        driver.getGraveyard(activePlayer) shouldHaveSize 2
        driver.getGraveyard(activePlayer) shouldBe listOf(extra1, extra2)
    }

    test("turn advances to next player after discard") {
        val driver = createDriver()
        val startingPlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(startingPlayer)

        // Add 1 extra card to hand
        val extraCard = driver.putCardInHand(startingPlayer, "Grizzly Bears")
        driver.getHandSize(startingPlayer) shouldBe 8

        // Pass through to cleanup
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // Select card to discard
        driver.submitCardSelection(startingPlayer, listOf(extraCard))

        // Turn should have advanced to opponent
        driver.activePlayer shouldBe opponent
    }

    test("discarding exactly the overflow amount") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        // Add 5 extra cards (7 + 5 = 12 total, must discard 5)
        val extraCards = mutableListOf<com.wingedsheep.sdk.model.EntityId>()
        repeat(5) {
            extraCards.add(driver.putCardInHand(activePlayer, "Grizzly Bears"))
        }

        driver.getHandSize(activePlayer) shouldBe 12

        // Pass through to cleanup
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // Should require exactly 5 discards
        val decision = driver.pendingDecision as SelectCardsDecision
        decision.minSelections shouldBe 5
        decision.maxSelections shouldBe 5

        // Discard the 5 extra cards
        driver.submitCardSelection(activePlayer, extraCards)

        // Hand should be exactly 7
        driver.getHandSize(activePlayer) shouldBe 7
    }

    test("player can choose which cards to discard") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        // Get the current hand cards
        val originalHand = driver.getHand(activePlayer).toList()

        // Add one extra card
        driver.putCardInHand(activePlayer, "Grizzly Bears")

        driver.getHandSize(activePlayer) shouldBe 8

        // Pass through to cleanup
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // Get the decision options - should be all 8 cards
        val decision = driver.pendingDecision as SelectCardsDecision
        decision.options shouldHaveSize 8

        // Choose to discard the first card from original hand (not the new one)
        val cardToDiscard = originalHand.first()
        driver.submitCardSelection(activePlayer, listOf(cardToDiscard))

        // Card should be in graveyard
        driver.getGraveyard(activePlayer) shouldHaveSize 1
        driver.getGraveyard(activePlayer).first() shouldBe cardToDiscard
    }
})
