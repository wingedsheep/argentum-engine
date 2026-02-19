package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Demystify.
 *
 * Demystify: {W}
 * Instant
 * Destroy target enchantment.
 */
class DemystifyTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Demystify destroys target enchantment") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put an enchantment on opponent's battlefield
        val enchantment = driver.putPermanentOnBattlefield(opponent, "Test Enchantment")
        driver.findPermanent(opponent, "Test Enchantment") shouldNotBe null

        // Give active player Demystify and mana
        val demystify = driver.putCardInHand(activePlayer, "Demystify")
        driver.giveMana(activePlayer, Color.WHITE, 1)

        // Cast Demystify targeting the enchantment
        val castResult = driver.castSpell(activePlayer, demystify, listOf(enchantment))
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // The enchantment should be destroyed (in graveyard)
        driver.findPermanent(opponent, "Test Enchantment") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Test Enchantment"
    }

    test("Demystify cannot target creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on opponent's battlefield (not an enchantment)
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Give active player Demystify and mana
        val demystify = driver.putCardInHand(activePlayer, "Demystify")
        driver.giveMana(activePlayer, Color.WHITE, 1)

        // Trying to cast Demystify targeting a creature should fail
        val castResult = driver.castSpell(activePlayer, demystify, listOf(creature))
        castResult.isSuccess shouldBe false

        // Grizzly Bears should still be on the battlefield
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }
})
