package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.QuickStudy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Quick Study — {2}{U} Instant — "Draw two cards."
 */
class QuickStudyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(QuickStudy)
        return driver
    }

    test("draws two cards on resolution") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(player)
        val quickStudy = driver.putCardInHand(player, "Quick Study")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 2)
        driver.castSpell(player, quickStudy)
        driver.bothPass() // resolve Quick Study

        // +1 (the card put in hand) is cast (-1), then draw 2 => net +2 over baseline.
        driver.getHandSize(player) shouldBe handBefore + 2
    }
})
