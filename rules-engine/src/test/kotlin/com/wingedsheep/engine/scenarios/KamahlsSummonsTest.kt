package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Kamahl's Summons.
 *
 * Kamahl's Summons: {3}{G}
 * Sorcery
 * Each player may reveal any number of creature cards from their hand.
 * Then each player creates a 2/2 green Bear creature token for each card
 * they revealed this way.
 */
class KamahlsSummonsTest : FunSpec({

    val KamahlsSummons = CardDefinition.sorcery(
        name = "Kamahl's Summons",
        manaCost = ManaCost.parse("{3}{G}"),
        oracleText = "Each player may reveal any number of creature cards from their hand. Then each player creates a 2/2 green Bear creature token for each card they revealed this way.",
        script = CardScript.spell(
            effect = Effects.EachPlayerRevealCreaturesCreateTokens(
                tokenPower = 2,
                tokenToughness = 2,
                tokenColors = setOf(Color.GREEN),
                tokenCreatureTypes = setOf("Bear")
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KamahlsSummons))
        return driver
    }

    test("each player reveals creatures and gets Bear tokens") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give each player creature cards in hand
        val bears1 = driver.putCardInHand(activePlayer, "Grizzly Bears")
        val courser = driver.putCardInHand(activePlayer, "Centaur Courser")
        val bears2 = driver.putCardInHand(opponent, "Grizzly Bears")

        // Cast Kamahl's Summons
        val summons = driver.putCardInHand(activePlayer, "Kamahl's Summons")
        driver.giveMana(activePlayer, Color.GREEN, 4)
        driver.castSpell(activePlayer, summons)
        driver.bothPass()

        // Active player should be asked first (APNAP order)
        val decision1 = driver.pendingDecision
        decision1.shouldBeInstanceOf<SelectCardsDecision>()
        decision1.playerId shouldBe activePlayer

        // Active player reveals both creature cards
        driver.submitCardSelection(activePlayer, listOf(bears1, courser))

        // Opponent should be asked next
        val decision2 = driver.pendingDecision
        decision2.shouldBeInstanceOf<SelectCardsDecision>()
        decision2.playerId shouldBe opponent

        // Opponent reveals one creature card
        driver.submitCardSelection(opponent, listOf(bears2))

        // Active player should have 2 Bear tokens
        val activeCreatures = driver.getCreatures(activePlayer)
        activeCreatures.shouldHaveSize(2)

        // Opponent should have 1 Bear token
        val opponentCreatures = driver.getCreatures(opponent)
        opponentCreatures.shouldHaveSize(1)
    }

    test("player can choose to reveal no creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Both players have creature cards
        val bears1 = driver.putCardInHand(activePlayer, "Grizzly Bears")
        val bears2 = driver.putCardInHand(opponent, "Grizzly Bears")

        // Cast Kamahl's Summons
        val summons = driver.putCardInHand(activePlayer, "Kamahl's Summons")
        driver.giveMana(activePlayer, Color.GREEN, 4)
        driver.castSpell(activePlayer, summons)
        driver.bothPass()

        // Active player reveals nothing
        driver.submitCardSelection(activePlayer, emptyList())

        // Opponent reveals one
        driver.submitCardSelection(opponent, listOf(bears2))

        // Active player should have 0 tokens
        driver.getCreatures(activePlayer).shouldHaveSize(0)

        // Opponent should have 1 Bear token
        driver.getCreatures(opponent).shouldHaveSize(1)
    }

    test("player with no creature cards in hand is skipped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only active player has creature cards - opponent has none
        val bears = driver.putCardInHand(activePlayer, "Grizzly Bears")

        // Cast Kamahl's Summons
        val summons = driver.putCardInHand(activePlayer, "Kamahl's Summons")
        driver.giveMana(activePlayer, Color.GREEN, 4)
        driver.castSpell(activePlayer, summons)
        driver.bothPass()

        // Only active player should be asked (opponent is skipped)
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe activePlayer

        // Active player reveals the creature
        driver.submitCardSelection(activePlayer, listOf(bears))

        // Active player should have 1 Bear token
        driver.getCreatures(activePlayer).shouldHaveSize(1)

        // Opponent should have 0 tokens
        driver.getCreatures(opponent).shouldHaveSize(0)
    }

    test("no players have creatures - no tokens created") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Neither player has creature cards in hand

        // Cast Kamahl's Summons
        val summons = driver.putCardInHand(activePlayer, "Kamahl's Summons")
        driver.giveMana(activePlayer, Color.GREEN, 4)
        driver.castSpell(activePlayer, summons)
        driver.bothPass()

        // Both players skipped - no decision needed
        driver.isPaused shouldBe false

        // No tokens created
        driver.getCreatures(activePlayer).shouldHaveSize(0)
        driver.getCreatures(opponent).shouldHaveSize(0)
    }
})
