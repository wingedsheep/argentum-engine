package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.OverwhelmingInstinct
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Overwhelming Instinct.
 *
 * Overwhelming Instinct: {2}{G}
 * Enchantment
 * Whenever you attack with three or more creatures, draw a card.
 */
class OverwhelmingInstinctTest : FunSpec({

    val TestCreature = CardDefinition.creature(
        name = "Test Creature",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestCreature))
        return driver
    }

    test("draws a card when attacking with exactly three creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        // Put Overwhelming Instinct on battlefield
        driver.putPermanentOnBattlefield(player, "Overwhelming Instinct")

        // Put three creatures on battlefield
        val c1 = driver.putCreatureOnBattlefield(player, "Test Creature")
        driver.removeSummoningSickness(c1)
        val c2 = driver.putCreatureOnBattlefield(player, "Test Creature")
        driver.removeSummoningSickness(c2)
        val c3 = driver.putCreatureOnBattlefield(player, "Test Creature")
        driver.removeSummoningSickness(c3)

        val handBefore = driver.getHandSize(player)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with all three creatures
        driver.declareAttackers(player, listOf(c1, c2, c3), opponent)

        // Resolve the triggered ability
        driver.bothPass()

        // Should have drawn one card
        driver.getHandSize(player) shouldBe handBefore + 1
    }

    test("draws a card when attacking with more than three creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putPermanentOnBattlefield(player, "Overwhelming Instinct")

        val creatures = (1..4).map {
            val c = driver.putCreatureOnBattlefield(player, "Test Creature")
            driver.removeSummoningSickness(c)
            c
        }

        val handBefore = driver.getHandSize(player)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, creatures, opponent)

        // Resolve the triggered ability
        driver.bothPass()

        // Should still draw exactly one card
        driver.getHandSize(player) shouldBe handBefore + 1
    }

    test("does not draw when attacking with fewer than three creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putPermanentOnBattlefield(player, "Overwhelming Instinct")

        val c1 = driver.putCreatureOnBattlefield(player, "Test Creature")
        driver.removeSummoningSickness(c1)
        val c2 = driver.putCreatureOnBattlefield(player, "Test Creature")
        driver.removeSummoningSickness(c2)

        val handBefore = driver.getHandSize(player)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(c1, c2), opponent)

        // Pass priority - no trigger should fire
        driver.bothPass()

        // Hand size should not change
        driver.getHandSize(player) shouldBe handBefore
    }

    test("does not draw when attacking with only one creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putPermanentOnBattlefield(player, "Overwhelming Instinct")

        val c1 = driver.putCreatureOnBattlefield(player, "Test Creature")
        driver.removeSummoningSickness(c1)

        val handBefore = driver.getHandSize(player)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(c1), opponent)

        // Pass priority - no trigger should fire
        driver.bothPass()

        // Hand size should not change
        driver.getHandSize(player) shouldBe handBefore
    }
})
