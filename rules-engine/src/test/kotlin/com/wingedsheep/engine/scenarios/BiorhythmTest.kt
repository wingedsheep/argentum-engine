package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Biorhythm.
 *
 * Biorhythm: {6}{G}{G}
 * Sorcery
 * Each player's life total becomes the number of creatures they control.
 */
class BiorhythmTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Biorhythm sets each player's life to their creature count") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player has 3 creatures, opponent has 1
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        driver.putCreatureOnBattlefield(activePlayer, "Wind Drake")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast Biorhythm
        val biorhythm = driver.putCardInHand(activePlayer, "Biorhythm")
        driver.giveMana(activePlayer, Color.GREEN, 8)

        val castResult = driver.castSpell(activePlayer, biorhythm)
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Active player's life should be 3 (controls 3 creatures)
        driver.getLifeTotal(activePlayer) shouldBe 3
        // Opponent's life should be 1 (controls 1 creature)
        driver.getLifeTotal(opponent) shouldBe 1
    }

    test("Biorhythm with no creatures sets life to 0 and player loses") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player has 1 creature, opponent has none
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Cast Biorhythm
        val biorhythm = driver.putCardInHand(activePlayer, "Biorhythm")
        driver.giveMana(activePlayer, Color.GREEN, 8)

        driver.castSpell(activePlayer, biorhythm)
        driver.bothPass()

        // Active player's life should be 1
        driver.getLifeTotal(activePlayer) shouldBe 1
        // Opponent's life should be 0 (no creatures)
        driver.getLifeTotal(opponent) shouldBe 0
    }

    test("Biorhythm does not change life if already equal to creature count") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 2
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Both players have 2 creatures, life is already 2
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        driver.putCreatureOnBattlefield(opponent, "Wind Drake")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val biorhythm = driver.putCardInHand(activePlayer, "Biorhythm")
        driver.giveMana(activePlayer, Color.GREEN, 8)

        driver.castSpell(activePlayer, biorhythm)
        driver.bothPass()

        // Both should still be at 2
        driver.getLifeTotal(activePlayer) shouldBe 2
        driver.getLifeTotal(opponent) shouldBe 2
    }

    test("Biorhythm only counts creatures, not other permanents") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player has 1 creature + lands (lands should not count)
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Forest")

        val biorhythm = driver.putCardInHand(activePlayer, "Biorhythm")
        driver.giveMana(activePlayer, Color.GREEN, 8)

        driver.castSpell(activePlayer, biorhythm)
        driver.bothPass()

        // Active player's life should be 1 (only the creature counts)
        driver.getLifeTotal(activePlayer) shouldBe 1
        // Opponent has no creatures
        driver.getLifeTotal(opponent) shouldBe 0
    }
})
