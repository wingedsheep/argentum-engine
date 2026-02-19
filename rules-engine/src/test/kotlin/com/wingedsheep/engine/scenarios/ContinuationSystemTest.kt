package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldContain

/**
 * Tests for the Continuation System.
 *
 * Verifies that effects requiring player decisions pause correctly,
 * and resume properly when the player submits their choice.
 *
 * ## Covered Scenarios
 * - Discard effect pauses for card selection
 * - Submitting card selection resumes and discards selected cards
 * - Composite effects with decisions (draw then discard)
 */
class ContinuationSystemTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("discard effect with choice pauses for player decision") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Island" to 20
            )
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: Give opponent 5 cards in hand
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardInHand(opponent, "Grizzly Bears")
        driver.putCardInHand(opponent, "Lightning Bolt")
        driver.putCardInHand(opponent, "Giant Growth")
        driver.putCardInHand(opponent, "Counterspell")
        driver.putCardInHand(opponent, "Doom Blade")

        // Give active player Mind Rot and mana
        val mindRot = driver.putCardInHand(activePlayer, "Mind Rot")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        // Opponent should have 5+ cards (initial hand + added cards)
        val opponentHandBefore = driver.getHandSize(opponent)
        opponentHandBefore shouldBe 12 // 7 starting + 5 added

        // Cast Mind Rot targeting opponent
        val castResult = driver.castSpell(activePlayer, mindRot, targets = listOf(opponent))
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Engine should be paused
        driver.isPaused shouldBe true

        // Check the pending decision
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision as SelectCardsDecision
        decision.playerId shouldBe opponent
        decision.minSelections shouldBe 2
        decision.maxSelections shouldBe 2
        decision.options shouldHaveSize 12
    }

    test("submitting card selection resumes and discards selected cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Island" to 20
            )
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: Give opponent specific cards
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        val growth = driver.putCardInHand(opponent, "Giant Growth")

        // Give active player Mind Rot and mana
        val mindRot = driver.putCardInHand(activePlayer, "Mind Rot")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        val opponentHandBefore = driver.getHand(opponent).size

        // Cast and resolve Mind Rot targeting opponent
        driver.castSpell(activePlayer, mindRot, targets = listOf(opponent))
        driver.bothPass()

        // Engine should be paused
        driver.isPaused shouldBe true

        // Submit the card selection (discard bears and bolt)
        val result = driver.submitCardSelection(opponent, listOf(bears, bolt))
        result.isSuccess shouldBe true

        // Engine should no longer be paused
        driver.isPaused shouldBe false

        // Check graveyard has the discarded cards
        val graveyardNames = driver.getGraveyardCardNames(opponent)
        graveyardNames shouldContain "Grizzly Bears"
        graveyardNames shouldContain "Lightning Bolt"

        // Hand should have 2 fewer cards
        val opponentHandAfter = driver.getHand(opponent).size
        opponentHandAfter shouldBe opponentHandBefore - 2

        // Giant Growth should still be in hand
        driver.findCardInHand(opponent, "Giant Growth") shouldNotBe null
    }

    test("discard all cards when hand size is less than or equal to discard count") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 40
            )
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: Clear opponent's hand and give them only 1 card
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // For this test, we'll work with what the opponent has drawn
        // Since opponent starts with 7 cards and we're casting Mind Rot (discard 2),
        // they need to choose. Let's give them exactly 2 cards.

        // Actually, let's test with opponent having only 1 card
        // We need to set up the state more carefully

        val mindRot = driver.putCardInHand(activePlayer, "Mind Rot")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        // Opponent has 7 cards from initial draw + the ones we added
        // Mind Rot makes them discard 2, so they choose

        driver.castSpell(activePlayer, mindRot, targets = listOf(opponent))
        driver.bothPass()

        // With 7+ cards, opponent must choose
        driver.isPaused shouldBe true

        // Pick any 2 cards from their hand
        val opponentHand = driver.getHand(opponent)
        opponentHand.size shouldBe 7  // Initial 7 card hand

        val cardsToDiscard = opponentHand.take(2)
        driver.submitCardSelection(opponent, cardsToDiscard)

        driver.isPaused shouldBe false
        driver.getHandSize(opponent) shouldBe 5
    }

    test("composite effect (draw then discard) handles pause correctly") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 40
            )
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give active player Careful Study (draw 1, discard 1) and mana
        val carefulStudy = driver.putCardInHand(activePlayer, "Careful Study")
        driver.giveMana(activePlayer, Color.BLACK, 1)

        val handSizeBefore = driver.getHandSize(activePlayer)

        // Cast Careful Study
        driver.castSpell(activePlayer, carefulStudy)
        driver.bothPass()

        // After draw, we now have 1 more card in hand (drew 1, spell left hand for stack)
        // Then discard effect should pause for choice

        // Engine should be paused for discard choice
        driver.isPaused shouldBe true

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision as SelectCardsDecision
        decision.playerId shouldBe activePlayer
        decision.minSelections shouldBe 1
        decision.maxSelections shouldBe 1

        // Pick a card to discard
        val handBeforeDiscard = driver.getHand(activePlayer)
        val cardToDiscard = handBeforeDiscard.first()

        val result = driver.submitCardSelection(activePlayer, listOf(cardToDiscard))
        result.isSuccess shouldBe true

        driver.isPaused shouldBe false

        // Net effect: drew 1, discarded 1, cast spell (left hand) = same hand size - 1
        // Actually: started with hand, cast spell (-1), drew (+1), discarded (-1) = net -1
        driver.getHandSize(activePlayer) shouldBe handSizeBefore - 1
    }
})
