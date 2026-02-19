package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Oblation.
 *
 * Oblation: {2}{W}
 * Instant
 * The owner of target nonland permanent shuffles it into their library, then draws two cards.
 */
class OblationTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Oblation shuffles target creature into owner's library and owner draws two cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on opponent's battlefield
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null

        val opponentHandBefore = driver.getHandSize(opponent)
        val opponentLibraryBefore = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size

        // Cast Oblation targeting opponent's creature
        val oblation = driver.putCardInHand(activePlayer, "Oblation")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        val castResult = driver.castSpell(activePlayer, oblation, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Creature should no longer be on battlefield
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null

        // Creature should not be in graveyard (it was shuffled into library)
        driver.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe false

        // Opponent's library should have grown by 1 (creature shuffled in) minus 2 (drew 2 cards) = net -1
        val opponentLibraryAfter = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size
        opponentLibraryAfter shouldBe opponentLibraryBefore - 1

        // Opponent drew 2 cards
        driver.getHandSize(opponent) shouldBe opponentHandBefore + 2
    }

    test("Oblation on own creature shuffles into library and you draw two cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on active player's battlefield
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val handBefore = driver.getHandSize(activePlayer)

        // Cast Oblation targeting own creature
        val oblation = driver.putCardInHand(activePlayer, "Oblation")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        val castResult = driver.castSpell(activePlayer, oblation, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Creature should be gone from battlefield
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null

        // Active player drew 2 cards (owner of the targeted permanent)
        // Hand: +1 (put oblation) -1 (cast oblation) +2 (draw) = net +2
        driver.getHandSize(activePlayer) shouldBe handBefore + 2
    }

    test("Oblation cannot target a land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a land on the battlefield
        val plains = driver.putPermanentOnBattlefield(activePlayer, "Plains")

        // Try to cast Oblation targeting the land
        val oblation = driver.putCardInHand(activePlayer, "Oblation")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        val castResult = driver.castSpell(activePlayer, oblation, listOf(plains))
        // Should fail - lands are not valid targets for Oblation
        castResult.isSuccess shouldBe false
    }
})
