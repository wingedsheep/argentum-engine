package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.AzusaLostButSeeking
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Azusa, Lost but Seeking — "You may play two additional lands on each of your turns."
 *
 * `GrantAdditionalLandDrop(count = 2)` for the controller only: three land drops on your own
 * turn, and the opponent keeps the normal single drop on theirs.
 */
class AzusaLostButSeekingScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AzusaLostButSeeking)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("the controller may play three lands on their turn, but not four") {
        val driver = newGame()
        val controller = driver.player1
        driver.putPermanentOnBattlefield(controller, "Azusa, Lost but Seeking")

        val lands = driver.getHand(controller).take(4)
        driver.playLand(controller, lands[0])
        driver.playLand(controller, lands[1])
        driver.playLand(controller, lands[2])
        withClue("Three land drops: the normal one plus Azusa's two") {
            driver.submitExpectFailure(PlayLand(controller, lands[3]))
        }
    }

    test("the opponent still gets only one land drop on their own turn") {
        val driver = newGame()
        val controller = driver.player1
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(controller, "Azusa, Lost but Seeking")

        driver.passPriorityUntil(Step.DRAW)
        withClue("Now on the opponent's turn") { driver.activePlayer shouldBe opponent }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lands = driver.getHand(opponent).take(2)
        driver.playLand(opponent, lands[0])
        withClue("Azusa grants the extra drops only to her controller") {
            driver.submitExpectFailure(PlayLand(opponent, lands[1]))
        }
    }
})
