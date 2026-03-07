package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.MarduSkullhunter
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MarduSkullhunterTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MarduSkullhunter)
        return driver
    }

    test("Mardu Skullhunter enters the battlefield tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(activePlayer, "Mardu Skullhunter")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        driver.castSpell(activePlayer, card)
        driver.bothPass()

        val permanent = driver.findPermanent(activePlayer, "Mardu Skullhunter")
        permanent shouldNotBe null
        driver.isTapped(permanent!!) shouldBe true
    }
})
