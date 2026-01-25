package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Enter the Battlefield (ETB) triggered abilities.
 *
 * ## Covered Scenarios
 * - Venerable Monk ETB: "When Venerable Monk enters the battlefield, you gain 2 life."
 */
class EntersBattlefieldTriggerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Venerable Monk ETB trigger grants 2 life when cast") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Verify starting life
        driver.getLifeTotal(activePlayer) shouldBe 20

        // Give active player Venerable Monk and mana to cast it
        val monk = driver.putCardInHand(activePlayer, "Venerable Monk")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        // Cast Venerable Monk
        val castResult = driver.castSpell(activePlayer, monk)
        castResult.isSuccess shouldBe true

        // Let the spell resolve (both players pass priority)
        driver.bothPass()

        // Monk should be on the battlefield
        driver.findPermanent(activePlayer, "Venerable Monk") shouldNotBe null

        // The ETB trigger should have resolved, granting 2 life
        // Note: The trigger goes on the stack and resolves after the creature enters
        // If there's a pending trigger on the stack, resolve it
        if (driver.stackSize > 0) {
            driver.bothPass() // Resolve the ETB trigger
        }

        // Player should have gained 2 life
        driver.getLifeTotal(activePlayer) shouldBe 22
    }

    test("Venerable Monk ETB trigger fires for the controller, not opponent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Verify starting life for both players
        driver.getLifeTotal(activePlayer) shouldBe 20
        driver.getLifeTotal(opponent) shouldBe 20

        // Give active player Venerable Monk and mana to cast it
        val monk = driver.putCardInHand(activePlayer, "Venerable Monk")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        // Cast Venerable Monk
        driver.castSpell(activePlayer, monk)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger if on stack
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Active player should have gained 2 life
        driver.getLifeTotal(activePlayer) shouldBe 22
        // Opponent should not have gained any life
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
