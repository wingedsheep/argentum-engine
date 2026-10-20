package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spiritual Sanctuary [LEG 38] — "At the beginning of each player's upkeep, if that player controls
 * a Plains, they gain 1 life."
 *
 * Symmetric: the upkeep player (not the Sanctuary's controller) is checked for a Plains and gains
 * the life.
 */
class SpiritualSanctuaryScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("an opponent who controls a Plains gains 1 life at their upkeep") {
        val driver = newGame()
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(driver.player1, "Spiritual Sanctuary")
        driver.putPermanentOnBattlefield(opponent, "Plains")
        val lifeBefore = driver.getLifeTotal(opponent)

        driver.passPriorityUntil(Step.DRAW)
        withClue("The opponent's upkeep trigger checks the opponent's Plains") {
            driver.activePlayer shouldBe opponent
            driver.getLifeTotal(opponent) shouldBe lifeBefore + 1
            driver.getLifeTotal(driver.player1) shouldBe lifeBefore
        }
    }

    test("a player without a Plains gains nothing, even when the Sanctuary's controller has one") {
        val driver = newGame()
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(driver.player1, "Spiritual Sanctuary")
        driver.putPermanentOnBattlefield(driver.player1, "Plains")
        val lifeBefore = driver.getLifeTotal(opponent)

        driver.passPriorityUntil(Step.DRAW)
        withClue("The intervening 'if' reads the upkeep player's battlefield, not the controller's") {
            driver.activePlayer shouldBe opponent
            driver.getLifeTotal(opponent) shouldBe lifeBefore
            driver.getLifeTotal(driver.player1) shouldBe lifeBefore
        }
    }
})
