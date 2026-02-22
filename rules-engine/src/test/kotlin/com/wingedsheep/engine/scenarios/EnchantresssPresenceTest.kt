package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.EnchantresssPresence
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Enchantress's Presence.
 *
 * Enchantress's Presence: {2}{G}
 * Enchantment
 * Whenever you cast an enchantment spell, draw a card.
 */
class EnchantresssPresenceTest : FunSpec({

    val TestEnchantment = card("Test Enchantment") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        spell {
            effect = GainLifeEffect(1)
        }
    }

    val TestCreatureSpell = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    val TestInstant = card("Test Instant") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            effect = GainLifeEffect(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestEnchantment, TestCreatureSpell, TestInstant))
        return driver
    }

    test("draws a card when casting an enchantment spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!

        // Put Enchantress's Presence on battlefield
        driver.putPermanentOnBattlefield(player, "Enchantress's Presence")

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(player)

        // Put an enchantment in hand and give mana
        val enchantment = driver.putCardInHand(player, "Test Enchantment")
        driver.giveMana(player, Color.GREEN, 1)

        // Cast the enchantment
        driver.castSpell(player, enchantment)

        // Resolve the trigger (Enchantress's Presence draws a card)
        driver.bothPass()

        // Resolve the enchantment spell itself
        driver.bothPass()

        // Hand should have increased by 1 (cast enchantment -1, trigger draw +1)
        driver.getHandSize(player) shouldBe handBefore + 1
    }

    test("does not draw when casting a creature spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!

        // Put Enchantress's Presence on battlefield
        driver.putPermanentOnBattlefield(player, "Enchantress's Presence")

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(player)

        // Put a creature in hand and give mana
        val creature = driver.putCardInHand(player, "Test Bear")
        driver.giveMana(player, Color.GREEN, 2)

        // Cast the creature
        driver.castSpell(player, creature)
        driver.bothPass()

        // Hand should have decreased by 1 (cast creature, no trigger)
        driver.getHandSize(player) shouldBe handBefore
    }

    test("does not draw when casting an instant spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!

        // Put Enchantress's Presence on battlefield
        driver.putPermanentOnBattlefield(player, "Enchantress's Presence")

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(player)

        // Put an instant in hand and give mana
        val instant = driver.putCardInHand(player, "Test Instant")
        driver.giveMana(player, Color.GREEN, 1)

        // Cast the instant
        driver.castSpell(player, instant)
        driver.bothPass()

        // Hand should have decreased by 1 (cast instant, no trigger)
        driver.getHandSize(player) shouldBe handBefore
    }

    test("draws multiple cards when casting multiple enchantments") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!

        // Put Enchantress's Presence on battlefield
        driver.putPermanentOnBattlefield(player, "Enchantress's Presence")

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(player)

        // Cast first enchantment
        val enchantment1 = driver.putCardInHand(player, "Test Enchantment")
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, enchantment1)
        driver.bothPass() // resolve trigger
        driver.bothPass() // resolve spell

        // Cast second enchantment
        val enchantment2 = driver.putCardInHand(player, "Test Enchantment")
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, enchantment2)
        driver.bothPass() // resolve trigger
        driver.bothPass() // resolve spell

        // Hand should have increased by 2 (cast 2 enchantments -2, trigger draws +2)
        driver.getHandSize(player) shouldBe handBefore + 2
    }
})
