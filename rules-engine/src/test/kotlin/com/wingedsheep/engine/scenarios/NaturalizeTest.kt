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
 * Tests for Naturalize.
 *
 * Naturalize: {1}{G}
 * Instant
 * Destroy target artifact or enchantment.
 */
class NaturalizeTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Naturalize destroys target enchantment") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val enchantment = driver.putPermanentOnBattlefield(opponent, "Test Enchantment")
        driver.findPermanent(opponent, "Test Enchantment") shouldNotBe null

        val naturalize = driver.putCardInHand(activePlayer, "Naturalize")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val castResult = driver.castSpell(activePlayer, naturalize, listOf(enchantment))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        driver.findPermanent(opponent, "Test Enchantment") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Test Enchantment"
    }

    test("Naturalize destroys target artifact creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val artifact = driver.putCreatureOnBattlefield(opponent, "Artifact Creature")
        driver.findPermanent(opponent, "Artifact Creature") shouldNotBe null

        val naturalize = driver.putCardInHand(activePlayer, "Naturalize")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val castResult = driver.castSpell(activePlayer, naturalize, listOf(artifact))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        driver.findPermanent(opponent, "Artifact Creature") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Artifact Creature"
    }

    test("Naturalize cannot target non-artifact non-enchantment creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val naturalize = driver.putCardInHand(activePlayer, "Naturalize")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val castResult = driver.castSpell(activePlayer, naturalize, listOf(creature))
        castResult.isSuccess shouldBe false

        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }
})
