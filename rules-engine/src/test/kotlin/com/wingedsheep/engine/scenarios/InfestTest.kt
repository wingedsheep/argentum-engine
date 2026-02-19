package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
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
 * Tests for Infest.
 *
 * Infest: {1}{B}{B}
 * Sorcery
 * All creatures get -2/-2 until end of turn.
 */
class InfestTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Infest kills creatures with toughness 2 or less") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a 2/2 on opponent's battlefield
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null

        // Cast Infest
        val infest = driver.putCardInHand(activePlayer, "Infest")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        val castResult = driver.castSpell(activePlayer, infest)
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Grizzly Bears (2/2) should die from -2/-2 (0/0 = lethal)
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
    }

    test("Infest shrinks but does not kill larger creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a 3/3 on opponent's battlefield
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Cast Infest
        val infest = driver.putCardInHand(activePlayer, "Infest")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        driver.castSpell(activePlayer, infest)
        driver.bothPass()

        // Centaur Courser (3/3) should survive as 1/1
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null
        projector.getProjectedPower(driver.state, courser) shouldBe 1
        projector.getProjectedToughness(driver.state, courser) shouldBe 1
    }

    test("Infest affects all creatures on both sides") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on both sides
        val myBears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val theirCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Cast Infest
        val infest = driver.putCardInHand(activePlayer, "Infest")
        driver.giveMana(activePlayer, Color.BLACK, 3)

        driver.castSpell(activePlayer, infest)
        driver.bothPass()

        // Active player's Grizzly Bears (2/2) should die
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Grizzly Bears"

        // Opponent's Centaur Courser (3/3) should survive as 1/1
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null
        projector.getProjectedPower(driver.state, theirCourser) shouldBe 1
        projector.getProjectedToughness(driver.state, theirCourser) shouldBe 1
    }
})
