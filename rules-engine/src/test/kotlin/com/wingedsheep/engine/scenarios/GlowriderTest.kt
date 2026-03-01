package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseSpellCostByFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Glowrider.
 *
 * Glowrider: {2}{W}
 * Creature — Human Cleric
 * 2/1
 * Noncreature spells cost {1} more to cast.
 */
class GlowriderTest : FunSpec({

    val Glowrider = CardDefinition.creature(
        name = "Glowrider",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 2,
        toughness = 1,
        oracleText = "Noncreature spells cost {1} more to cast.",
        script = CardScript(
            staticAbilities = listOf(
                IncreaseSpellCostByFilter(
                    filter = GameObjectFilter.Noncreature,
                    amount = 1
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Glowrider))
        return driver
    }

    test("Glowrider increases noncreature spell cost by 1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Glowrider on battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Glowrider")

        // Put Lightning Bolt in hand (costs {R} normally)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Give exactly 1 red mana — not enough with Glowrider's tax
        driver.giveMana(activePlayer, Color.RED, 1)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = bolt,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }

    test("noncreature spell can be cast with enough mana to pay tax") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Glowrider on battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Glowrider")

        // Put Test Enchantment in hand (costs {1}{W}, taxed to {2}{W})
        val enchantment = driver.putCardInHand(activePlayer, "Test Enchantment")

        // Give 3 white mana (enough for {2}{W})
        driver.giveMana(activePlayer, Color.WHITE, 3)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = enchantment,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }

    test("Glowrider does not increase creature spell cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Glowrider on battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Glowrider")

        // Put Grizzly Bears in hand (costs {1}{G})
        val bears = driver.putCardInHand(activePlayer, "Grizzly Bears")

        // Give exactly {1}{G} — should be enough since creatures aren't taxed
        driver.giveMana(activePlayer, Color.GREEN, 1)
        driver.giveMana(activePlayer, Color.WHITE, 1)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = bears,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }

    test("Glowrider taxes opponent's noncreature spells too") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Glowrider on active player's battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Glowrider")

        // Put Lightning Bolt in opponent's hand
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")

        // Give opponent only 1 red mana — not enough with tax
        driver.giveMana(opponent, Color.RED, 1)

        // Pass priority to opponent
        driver.passPriority(activePlayer)

        val result = driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = bolt,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }

    test("two Glowriders stack the tax effect") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two Glowriders on battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Glowrider")
        driver.putPermanentOnBattlefield(activePlayer, "Glowrider")

        // Put Lightning Bolt in hand
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Give {1}{R} — not enough with double tax (needs {2}{R})
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.WHITE, 1)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = bolt,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
