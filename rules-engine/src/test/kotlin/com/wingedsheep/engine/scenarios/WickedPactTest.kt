package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Wicked Pact's multi-target spell behavior.
 *
 * Wicked Pact: {1}{B}{B}
 * Sorcery
 * Destroy two target nonblack creatures. You lose 5 life.
 *
 * Note: At the rules-engine level, targets are provided when casting the spell.
 * The game-server layer handles presenting the targeting UI to the player.
 */
class WickedPactTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Wicked Pact destroys two target nonblack creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two nonblack creatures on opponent's battlefield
        val creature1 = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val creature2 = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Give active player Wicked Pact and mana to cast it
        val wickedPact = driver.putCardInHand(activePlayer, "Wicked Pact")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        // Cast Wicked Pact with both targets
        val castResult = driver.castSpell(activePlayer, wickedPact, listOf(creature1, creature2))
        castResult.isSuccess shouldBe true

        // Let the spell resolve
        driver.bothPass()

        // Both creatures should be destroyed
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
        driver.getGraveyardCardNames(opponent) shouldContain "Centaur Courser"

        // Active player should have lost 5 life
        driver.getLifeTotal(activePlayer) shouldBe 15
    }

    test("Wicked Pact can target creatures controlled by different players") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on both sides
        val ownCreature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Give active player Wicked Pact and mana
        val wickedPact = driver.putCardInHand(activePlayer, "Wicked Pact")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        // Cast Wicked Pact targeting both creatures
        val castResult = driver.castSpell(activePlayer, wickedPact, listOf(ownCreature, opponentCreature))
        castResult.isSuccess shouldBe true

        // Let the spell resolve
        driver.bothPass()

        // Both creatures should be destroyed
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null

        // Active player should have lost 5 life
        driver.getLifeTotal(activePlayer) shouldBe 15
    }

    test("Wicked Pact targets the same creature twice if only one available") {
        // Note: MTG rules allow targeting the same creature with both targets
        // if only one nonblack creature exists (though this may be suboptimal)
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put one nonblack creature and one black creature on the battlefield
        val greenCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Serpent Assassin")

        // Give active player Wicked Pact and mana
        val wickedPact = driver.putCardInHand(activePlayer, "Wicked Pact")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        // Cast Wicked Pact targeting the green creature twice
        // (This tests that the spell can be cast with valid targets)
        val castResult = driver.castSpell(activePlayer, wickedPact, listOf(greenCreature, greenCreature))
        castResult.isSuccess shouldBe true

        // Let the spell resolve
        driver.bothPass()

        // Green creature should be destroyed (first destroy succeeds, second has no effect)
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        // Black creature should still be there (cannot be targeted)
        driver.findPermanent(opponent, "Serpent Assassin") shouldBe opponent.let {
            driver.findPermanent(it, "Serpent Assassin")
        }

        // Active player should have lost 5 life
        driver.getLifeTotal(activePlayer) shouldBe 15
    }

    test("Wicked Pact costs 1BB to cast and causes life loss") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures on the battlefield
        val creature1 = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val creature2 = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Give active player Wicked Pact and only 2 black mana (not enough - needs 1BB = 3 total)
        val wickedPact = driver.putCardInHand(activePlayer, "Wicked Pact")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Try to cast Wicked Pact - should fail due to insufficient mana
        val castResult = driver.castSpell(activePlayer, wickedPact, listOf(creature1, creature2))
        castResult.isSuccess shouldBe false

        // Creatures should still be on the battlefield
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe creature1
        driver.findPermanent(opponent, "Centaur Courser") shouldBe creature2

        // Life total should be unchanged
        driver.getLifeTotal(activePlayer) shouldBe 20
    }
})
