package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.EachPlayerDiscardsOrLoseLifeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Strongarm Tactics.
 *
 * Strongarm Tactics: {1}{B}
 * Sorcery
 * Each player discards a card. Then each player who didn't discard a creature card this way loses 4 life.
 */
class StrongarmTacticsTest : FunSpec({

    val StrongarmTactics = CardDefinition.sorcery(
        name = "Strongarm Tactics",
        manaCost = ManaCost.parse("{1}{B}"),
        oracleText = "Each player discards a card. Then each player who didn't discard a creature card this way loses 4 life.",
        script = CardScript.spell(
            effect = EachPlayerDiscardsOrLoseLifeEffect(lifeLoss = 4)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StrongarmTactics))
        return driver
    }

    test("both players discard creature cards - no life loss") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creature cards in both players' hands
        val activeCreature = driver.putCardInHand(activePlayer, "Grizzly Bears")
        val opponentCreature = driver.putCardInHand(opponent, "Grizzly Bears")

        // Cast Strongarm Tactics
        val strongarm = driver.putCardInHand(activePlayer, "Strongarm Tactics")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        val castResult = driver.castSpell(activePlayer, strongarm)
        castResult.isSuccess shouldBe true

        // Resolve
        driver.bothPass()

        // Active player must choose a card to discard (has multiple cards)
        driver.isPaused shouldBe true
        driver.submitCardSelection(activePlayer, listOf(activeCreature))

        // Opponent must choose a card to discard
        driver.isPaused shouldBe true
        driver.submitCardSelection(opponent, listOf(opponentCreature))

        // Both discarded creatures - no life loss
        driver.getLifeTotal(activePlayer) shouldBe 20
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("player discards non-creature card loses 4 life") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature in active player's hand and a non-creature in opponent's hand
        val activeCreature = driver.putCardInHand(activePlayer, "Grizzly Bears")
        val opponentLand = driver.putCardInHand(opponent, "Forest")

        // Cast Strongarm Tactics
        val strongarm = driver.putCardInHand(activePlayer, "Strongarm Tactics")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        val castResult = driver.castSpell(activePlayer, strongarm)
        castResult.isSuccess shouldBe true

        // Resolve
        driver.bothPass()

        // Active player discards creature
        driver.isPaused shouldBe true
        driver.submitCardSelection(activePlayer, listOf(activeCreature))

        // Opponent discards land (non-creature)
        driver.isPaused shouldBe true
        driver.submitCardSelection(opponent, listOf(opponentLand))

        // Active player discarded creature - no life loss
        driver.getLifeTotal(activePlayer) shouldBe 20

        // Opponent discarded non-creature - loses 4 life
        driver.getLifeTotal(opponent) shouldBe 16

        // Verify life change event
        val lifeEvents = driver.events.filterIsInstance<LifeChangedEvent>()
        lifeEvents.any { it.playerId == opponent && it.newLife == 16 } shouldBe true
    }

    test("both players discard non-creature cards - both lose 4 life") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put non-creature cards in both players' hands
        val activeLand = driver.putCardInHand(activePlayer, "Forest")
        val opponentLand = driver.putCardInHand(opponent, "Swamp")

        // Cast Strongarm Tactics
        val strongarm = driver.putCardInHand(activePlayer, "Strongarm Tactics")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        val castResult = driver.castSpell(activePlayer, strongarm)
        castResult.isSuccess shouldBe true

        // Resolve
        driver.bothPass()

        // Active player discards non-creature
        driver.isPaused shouldBe true
        driver.submitCardSelection(activePlayer, listOf(activeLand))

        // Opponent discards non-creature
        driver.isPaused shouldBe true
        driver.submitCardSelection(opponent, listOf(opponentLand))

        // Both lose 4 life
        driver.getLifeTotal(activePlayer) shouldBe 16
        driver.getLifeTotal(opponent) shouldBe 16
    }

    test("discard events are emitted for both players") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activeCreature = driver.putCardInHand(activePlayer, "Grizzly Bears")
        val opponentCreature = driver.putCardInHand(opponent, "Grizzly Bears")

        val strongarm = driver.putCardInHand(activePlayer, "Strongarm Tactics")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        driver.castSpell(activePlayer, strongarm)
        driver.bothPass()

        driver.submitCardSelection(activePlayer, listOf(activeCreature))
        driver.submitCardSelection(opponent, listOf(opponentCreature))

        // Verify discard events for both players
        val discardEvents = driver.events.filterIsInstance<CardsDiscardedEvent>()
        discardEvents.any { it.playerId == activePlayer && it.cardIds.contains(activeCreature) } shouldBe true
        discardEvents.any { it.playerId == opponent && it.cardIds.contains(opponentCreature) } shouldBe true
    }
})
