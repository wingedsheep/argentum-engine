package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for GrantCantBeCountered static ability.
 *
 * Root Sliver: {3}{G}
 * Creature — Sliver
 * 2/2
 * This spell can't be countered.
 * Sliver spells can't be countered.
 */
class RootSliverTest : FunSpec({

    // A permanent that grants "Sliver spells can't be countered"
    val CantBeCounteredGranter = CardDefinition.creature(
        name = "Cant Be Countered Granter",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Sliver")),
        power = 1,
        toughness = 1,
        script = CardScript(
            staticAbilities = listOf(
                GrantCantBeCountered(
                    filter = GameObjectFilter.Any.withSubtype("Sliver")
                )
            )
        )
    )

    // A simple Sliver to test the granted ability
    val TestSliver = CardDefinition.creature(
        name = "Test Sliver",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Sliver")),
        power = 1,
        toughness = 1
    )

    // A non-Sliver creature to test that it's NOT protected
    val TestBear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CantBeCounteredGranter, TestSliver, TestBear))
        return driver
    }

    test("GrantCantBeCountered prevents Sliver spells from being countered") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put the granter on the battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Cant Be Countered Granter")

        // Put Sliver in hand and mana for both
        val sliver = driver.putCardInHand(activePlayer, "Test Sliver")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.giveMana(opponent, Color.BLUE, 2)

        // Cast Test Sliver
        driver.castSpell(activePlayer, sliver)
        driver.stackSize shouldBe 1

        // Pass to opponent
        driver.passPriority(activePlayer)

        // Opponent casts Counterspell targeting the Sliver
        val topSpellId = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(topSpellId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass - Counterspell resolves but Sliver can't be countered
        driver.bothPass()

        // Sliver should still be on the stack (not countered)
        driver.stackSize shouldBe 1
        driver.getTopOfStackName() shouldBe "Test Sliver"

        // Both pass - Sliver resolves and enters the battlefield
        driver.bothPass()

        driver.stackSize shouldBe 0
        driver.findPermanent(activePlayer, "Test Sliver") shouldNotBe null
    }

    test("GrantCantBeCountered does not protect non-Sliver spells") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put the granter on the battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Cant Be Countered Granter")

        // Put Bear in hand and mana for both
        val bear = driver.putCardInHand(activePlayer, "Test Bear")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.giveMana(opponent, Color.BLUE, 2)

        // Cast Test Bear
        driver.castSpell(activePlayer, bear)
        driver.stackSize shouldBe 1

        // Pass to opponent
        driver.passPriority(activePlayer)

        // Opponent casts Counterspell targeting the Bear
        val topSpellId = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(topSpellId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass - Counterspell resolves and Bear IS countered
        driver.bothPass()

        // Bear should be gone (countered)
        driver.stackSize shouldBe 0
        driver.findPermanent(activePlayer, "Test Bear") shouldBe null
    }

    test("GrantCantBeCountered only works while granter is on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No granter on the battlefield - Sliver can be countered
        val sliver = driver.putCardInHand(activePlayer, "Test Sliver")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.giveMana(opponent, Color.BLUE, 2)

        // Cast Test Sliver
        driver.castSpell(activePlayer, sliver)
        driver.stackSize shouldBe 1

        // Pass to opponent
        driver.passPriority(activePlayer)

        // Opponent casts Counterspell targeting the Sliver
        val topSpellId = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(topSpellId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass - Counterspell resolves and Sliver IS countered (no granter)
        driver.bothPass()

        // Sliver should be gone
        driver.stackSize shouldBe 0
        driver.findPermanent(activePlayer, "Test Sliver") shouldBe null
    }
})
