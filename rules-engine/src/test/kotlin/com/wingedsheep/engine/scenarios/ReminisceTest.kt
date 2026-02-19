package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Reminisce (ONS #105).
 *
 * Reminisce: {2}{U}
 * Sorcery
 * Target player shuffles their graveyard into their library.
 */
class ReminisceTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Reminisce shuffles target player's graveyard into their library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Island" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put some cards in opponent's graveyard
        driver.putCardInGraveyard(opponent, "Grizzly Bears")
        driver.putCardInGraveyard(opponent, "Centaur Courser")
        driver.putCardInGraveyard(opponent, "Wind Drake")

        val graveyardBefore = driver.getGraveyardCardNames(opponent)
        graveyardBefore.size shouldBe 3

        val libraryBefore = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size

        // Cast Reminisce targeting opponent
        driver.giveMana(activePlayer, Color.BLUE, 3)
        val reminisce = driver.putCardInHand(activePlayer, "Reminisce")
        val castResult = driver.castSpell(activePlayer, reminisce, listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Opponent's graveyard should be empty
        driver.getGraveyardCardNames(opponent).size shouldBe 0

        // Opponent's library should have grown by 3
        val libraryAfter = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size
        libraryAfter shouldBe libraryBefore + 3
    }

    test("Reminisce can target yourself") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Island" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put cards in our own graveyard
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Centaur Courser")

        val graveyardBefore = driver.getGraveyardCardNames(activePlayer)
        graveyardBefore.size shouldBe 2

        val libraryBefore = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY)).size

        // Cast Reminisce targeting self
        driver.giveMana(activePlayer, Color.BLUE, 3)
        val reminisce = driver.putCardInHand(activePlayer, "Reminisce")
        val castResult = driver.castSpell(activePlayer, reminisce, listOf(activePlayer))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Our graveyard should contain only Reminisce itself (moved there after resolving)
        val graveyardAfter = driver.getGraveyardCardNames(activePlayer)
        graveyardAfter.size shouldBe 1
        graveyardAfter[0] shouldBe "Reminisce"

        // Our library should have grown by 2 (the 2 graveyard cards shuffled in)
        val libraryAfter = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY)).size
        libraryAfter shouldBe libraryBefore + 2
    }

    test("Reminisce with empty graveyard still resolves") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Island" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent's graveyard is empty
        driver.getGraveyardCardNames(opponent).size shouldBe 0

        val libraryBefore = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size

        // Cast Reminisce targeting opponent
        driver.giveMana(activePlayer, Color.BLUE, 3)
        val reminisce = driver.putCardInHand(activePlayer, "Reminisce")
        val castResult = driver.castSpell(activePlayer, reminisce, listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Graveyard still empty, library size unchanged
        driver.getGraveyardCardNames(opponent).size shouldBe 0
        val libraryAfter = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size
        libraryAfter shouldBe libraryBefore
    }
})
