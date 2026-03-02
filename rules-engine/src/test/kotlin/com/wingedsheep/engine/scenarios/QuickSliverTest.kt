package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Flash keyword and GrantFlashToSpellType static ability.
 *
 * Quick Sliver: {1}{G}
 * Creature — Sliver
 * 1/1
 * Flash
 * Any player may cast Sliver spells as though they had flash.
 */
class QuickSliverTest : FunSpec({

    // A creature with Flash keyword
    val FlashCreature = CardDefinition.creature(
        name = "Flash Creature",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Sliver")),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.FLASH)
    )

    // A regular Sliver creature (no flash)
    val TestSliver = CardDefinition.creature(
        name = "Test Sliver",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Sliver")),
        power = 1,
        toughness = 1
    )

    // A non-Sliver creature (should not get flash from Quick Sliver)
    val TestBear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    // A permanent with GrantFlashToSpellType for Slivers
    val FlashGranter = CardDefinition.creature(
        name = "Flash Granter",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Sliver")),
        power = 1,
        toughness = 1,
        script = CardScript(
            staticAbilities = listOf(
                GrantFlashToSpellType(
                    filter = GameObjectFilter.Any.withSubtype("Sliver")
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlashCreature, TestSliver, TestBear, FlashGranter))
        return driver
    }

    test("creature with Flash keyword can be cast at instant speed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put the card in hand and give mana, then pass to upkeep (next turn)
        val flashCreature = driver.putCardInHand(activePlayer, "Flash Creature")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        // Pass to end step — not sorcery speed
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = flashCreature,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }

    test("creature without Flash cannot be cast at instant speed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCardInHand(activePlayer, "Test Bear")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        // Pass to end step — not sorcery speed
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = bear,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }

    test("GrantFlashToSpellType allows matching Sliver to be cast at instant speed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Flash Granter on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Flash Granter")

        // Try to cast a Sliver at end step
        val sliver = driver.putCardInHand(activePlayer, "Test Sliver")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = sliver,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }

    test("GrantFlashToSpellType does not grant flash to non-matching creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Flash Granter on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Flash Granter")

        // Try to cast a non-Sliver at end step — should fail
        val bear = driver.putCardInHand(activePlayer, "Test Bear")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = bear,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }

    test("GrantFlashToSpellType allows opponent to cast Sliver at instant speed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Flash Granter on active player's battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Flash Granter")

        // Opponent gets a Sliver in hand
        val sliver = driver.putCardInHand(opponent, "Test Sliver")
        driver.giveMana(opponent, Color.RED, 2)

        // Advance to end step
        driver.passPriorityUntil(Step.END)

        // Pass priority to opponent
        driver.passPriority(activePlayer)

        val result = driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = sliver,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }
})
