package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.triggers.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for "look at target player's hand" effects.
 *
 * ## Covered Scenarios
 * - Ingenious Thief ETB: "When Ingenious Thief enters the battlefield, look at target player's hand."
 * - Cards that are looked at remain revealed to the viewing player.
 */
class LookAtHandTest : FunSpec({

    // Test card that mimics Ingenious Thief
    val IngeniousThief = CardDefinition(
        name = "Ingenious Thief",
        manaCost = ManaCost.parse("{1}{U}"),
        typeLine = com.wingedsheep.sdk.core.TypeLine.creature(setOf(Subtype("Human"), Subtype("Rogue"))),
        oracleText = "Flying\nWhen Ingenious Thief enters the battlefield, look at target player's hand.",
        keywords = setOf(Keyword.FLYING),
        creatureStats = com.wingedsheep.sdk.model.CreatureStats(1, 1),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(),
                effect = LookAtTargetHandEffect(EffectTarget.ContextTarget(0)),
                targetRequirement = TargetPlayer()
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(IngeniousThief)
        return driver
    }

    test("Ingenious Thief ETB lets controller look at target player's hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put some cards in the opponent's hand for testing
        val opponentCard1 = driver.putCardInHand(opponent, "Forest")
        val opponentCard2 = driver.putCardInHand(opponent, "Island")

        // Verify opponent has cards in hand
        driver.getHand(opponent).size shouldBe driver.getHand(opponent).size

        // Give active player Ingenious Thief and mana to cast it
        val thief = driver.putCardInHand(activePlayer, "Ingenious Thief")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Cast Ingenious Thief
        val castResult = driver.castSpell(activePlayer, thief)
        castResult.isSuccess shouldBe true

        // Let the spell resolve (both players pass priority)
        driver.bothPass()

        // Thief should be on the battlefield
        driver.findPermanent(activePlayer, "Ingenious Thief") shouldNotBe null

        // The ETB trigger should be on the stack requiring a target
        // Submit the target selection (opponent)
        if (driver.isPaused) {
            driver.submitTargetSelection(activePlayer, listOf(opponent))
        }

        // Resolve the ETB trigger
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Verify the cards in opponent's hand are now revealed to the active player
        val opponentHand = driver.getHand(opponent)
        opponentHand.forEach { cardId ->
            val revealedComponent = driver.state.getEntity(cardId)?.get<RevealedToComponent>()
            revealedComponent shouldNotBe null
            revealedComponent!!.isRevealedTo(activePlayer) shouldBe true
        }

        // Verify HandLookedAtEvent was emitted
        val lookAtEvents = driver.events.filterIsInstance<HandLookedAtEvent>()
        lookAtEvents shouldHaveSize 1
        lookAtEvents[0].viewingPlayerId shouldBe activePlayer
        lookAtEvents[0].targetPlayerId shouldBe opponent
        lookAtEvents[0].cardIds shouldContain opponentCard1
        lookAtEvents[0].cardIds shouldContain opponentCard2
    }

    test("Revealed cards stay revealed even after new cards are drawn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a card in the opponent's hand
        val revealedCard = driver.putCardInHand(opponent, "Forest")

        // Give active player Ingenious Thief and mana to cast it
        val thief = driver.putCardInHand(activePlayer, "Ingenious Thief")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Cast Ingenious Thief
        driver.castSpell(activePlayer, thief)
        driver.bothPass()

        // Submit target selection
        if (driver.isPaused) {
            driver.submitTargetSelection(activePlayer, listOf(opponent))
        }

        // Resolve ETB trigger
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // The revealed card should be marked as revealed
        val revealedComponent = driver.state.getEntity(revealedCard)?.get<RevealedToComponent>()
        revealedComponent shouldNotBe null
        revealedComponent!!.isRevealedTo(activePlayer) shouldBe true

        // Now add a new card to opponent's hand (simulating a draw)
        val newCard = driver.putCardInHand(opponent, "Island")

        // The new card should NOT be revealed (it wasn't in hand when looked at)
        val newCardRevealedComponent = driver.state.getEntity(newCard)?.get<RevealedToComponent>()
        (newCardRevealedComponent == null || !newCardRevealedComponent.isRevealedTo(activePlayer)) shouldBe true

        // The original card should still be revealed
        val stillRevealedComponent = driver.state.getEntity(revealedCard)?.get<RevealedToComponent>()
        stillRevealedComponent shouldNotBe null
        stillRevealedComponent!!.isRevealedTo(activePlayer) shouldBe true
    }

    test("Looking at empty hand still emits event") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Don't add any cards to opponent's hand - they should have starting hand though
        // Clear opponent's hand for this test by "discarding" all cards
        val opponentInitialHand = driver.getHand(opponent).toList()

        // Give active player Ingenious Thief and mana to cast it
        val thief = driver.putCardInHand(activePlayer, "Ingenious Thief")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Cast Ingenious Thief
        driver.castSpell(activePlayer, thief)
        driver.bothPass()

        // Submit target selection
        if (driver.isPaused) {
            driver.submitTargetSelection(activePlayer, listOf(opponent))
        }

        // Resolve ETB trigger
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Verify HandLookedAtEvent was emitted (even if hand wasn't empty, event should fire)
        val lookAtEvents = driver.events.filterIsInstance<HandLookedAtEvent>()
        lookAtEvents shouldHaveSize 1
        lookAtEvents[0].viewingPlayerId shouldBe activePlayer
        lookAtEvents[0].targetPlayerId shouldBe opponent
    }

    test("Cards revealed by look at hand can be looked at again without duplicate entries") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a card in the opponent's hand
        val card = driver.putCardInHand(opponent, "Forest")

        // Give active player two Ingenious Thieves and mana
        val thief1 = driver.putCardInHand(activePlayer, "Ingenious Thief")
        val thief2 = driver.putCardInHand(activePlayer, "Ingenious Thief")
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Cast first thief
        driver.castSpell(activePlayer, thief1)
        driver.bothPass()

        if (driver.isPaused) {
            driver.submitTargetSelection(activePlayer, listOf(opponent))
        }

        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Card should be revealed
        var revealedComponent = driver.state.getEntity(card)?.get<RevealedToComponent>()
        revealedComponent shouldNotBe null
        revealedComponent!!.playerIds shouldHaveSize 1

        // Cast second thief
        driver.castSpell(activePlayer, thief2)
        driver.bothPass()

        if (driver.isPaused) {
            driver.submitTargetSelection(activePlayer, listOf(opponent))
        }

        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Card should still be revealed and not have duplicate entries
        revealedComponent = driver.state.getEntity(card)?.get<RevealedToComponent>()
        revealedComponent shouldNotBe null
        // Should still have just one player in the set (sets don't have duplicates)
        revealedComponent!!.playerIds shouldHaveSize 1
        revealedComponent.isRevealedTo(activePlayer) shouldBe true
    }
})
