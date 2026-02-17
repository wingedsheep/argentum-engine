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

/**
 * Tests for the Flux spell.
 *
 * Flux: {2}{U} Sorcery
 * Each player discards any number of cards, then draws that many cards. Draw a card.
 *
 * The effect requires:
 * 1. Active player (controller) selects cards to discard (0 or more)
 * 2. Then opponent selects cards to discard (0 or more)
 * 3. Both discard their selected cards
 * 4. Each player draws cards equal to what they discarded
 * 5. Controller draws 1 extra card
 */
class FluxTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Flux pauses for active player to select cards to discard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 30,
                "Grizzly Bears" to 10
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give active player Flux and mana
        val flux = driver.putCardInHand(activePlayer, "Flux")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast Flux
        val castResult = driver.castSpell(activePlayer, flux)
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Engine should be paused for active player's selection
        driver.isPaused shouldBe true

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision as SelectCardsDecision
        decision.playerId shouldBe activePlayer
        decision.minSelections shouldBe 0  // "any number" means 0 minimum
    }

    test("After active player selects, opponent is prompted") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 30,
                "Grizzly Bears" to 10
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give active player Flux and mana
        val flux = driver.putCardInHand(activePlayer, "Flux")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast and resolve Flux
        driver.castSpell(activePlayer, flux)
        driver.bothPass()

        // Active player selects 2 cards to discard
        val activeHand = driver.getHand(activePlayer)
        val activeCardsToDiscard = activeHand.take(2)
        driver.submitCardSelection(activePlayer, activeCardsToDiscard)

        // Should now be paused for opponent's selection
        driver.isPaused shouldBe true

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision as SelectCardsDecision
        decision.playerId shouldBe opponent
        decision.minSelections shouldBe 0
    }

    test("Players can discard zero cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 30,
                "Grizzly Bears" to 10
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activeHandBefore = driver.getHandSize(activePlayer)
        val opponentHandBefore = driver.getHandSize(opponent)

        // Give active player Flux and mana
        val flux = driver.putCardInHand(activePlayer, "Flux")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast and resolve Flux
        driver.castSpell(activePlayer, flux)
        driver.bothPass()

        // Active player selects 0 cards
        driver.submitCardSelection(activePlayer, emptyList())

        // Opponent selects 0 cards
        driver.submitCardSelection(opponent, emptyList())

        // Effect should be complete
        driver.isPaused shouldBe false

        // Active player:
        // - Started with 7 cards (activeHandBefore)
        // - Put Flux in hand: 8 cards
        // - Cast Flux: 7 cards (Flux on stack)
        // - Discarded 0, drew 0
        // - Drew 1 bonus: 8 cards
        driver.getHandSize(activePlayer) shouldBe activeHandBefore + 1  // +1 from bonus draw

        // Opponent: discarded 0, drew 0 = no change
        driver.getHandSize(opponent) shouldBe opponentHandBefore
    }

    test("Full Flux resolution with discards and draws") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 30,
                "Grizzly Bears" to 10
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give active player Flux and mana
        val flux = driver.putCardInHand(activePlayer, "Flux")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        val activeHandBefore = driver.getHandSize(activePlayer) // 7 + 1 (Flux) = 8
        val opponentHandBefore = driver.getHandSize(opponent) // 7

        // Cast and resolve Flux
        driver.castSpell(activePlayer, flux)
        driver.bothPass()

        // After casting, active player's hand is 7 (Flux moved to stack)
        // Active player selects 3 cards to discard
        val activeHand = driver.getHand(activePlayer)
        activeHand shouldHaveSize 7
        val activeCardsToDiscard = activeHand.take(3)
        driver.submitCardSelection(activePlayer, activeCardsToDiscard)

        // Opponent selects 2 cards to discard
        val opponentHand = driver.getHand(opponent)
        val opponentCardsToDiscard = opponentHand.take(2)
        driver.submitCardSelection(opponent, opponentCardsToDiscard)

        // Effect should be complete
        driver.isPaused shouldBe false

        // Active player:
        // - Started with 8 cards (7 + Flux)
        // - Cast Flux: 7 cards
        // - Discarded 3: 4 cards
        // - Drew 3: 7 cards
        // - Drew 1 bonus: 8 cards
        driver.getHandSize(activePlayer) shouldBe 8

        // Opponent:
        // - Started with 7 cards
        // - Discarded 2: 5 cards
        // - Drew 2: 7 cards
        driver.getHandSize(opponent) shouldBe 7

        // Check graveyards
        driver.getGraveyard(activePlayer) shouldHaveSize 4  // 3 discarded + Flux spell
        driver.getGraveyard(opponent) shouldHaveSize 2  // 2 discarded
    }

    test("Player with empty hand selects zero cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 30
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give active player Flux and mana
        val flux = driver.putCardInHand(activePlayer, "Flux")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast and resolve Flux
        driver.castSpell(activePlayer, flux)
        driver.bothPass()

        // Active player discards all 7 cards
        val activeHand = driver.getHand(activePlayer)
        activeHand shouldHaveSize 7
        driver.submitCardSelection(activePlayer, activeHand)

        // Opponent selects 0 cards (choosing not to discard)
        driver.submitCardSelection(opponent, emptyList())

        // Effect should be complete
        driver.isPaused shouldBe false

        // Active player: discarded 7, drew 7, drew 1 bonus = 8
        driver.getHandSize(activePlayer) shouldBe 8

        // Opponent: discarded 0, drew 0 = 7 (unchanged)
        driver.getHandSize(opponent) shouldBe 7
    }
})
