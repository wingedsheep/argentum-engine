package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.CantGainLifeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PsychicTransferScenarioTest : FunSpec({
    fun newGame(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for ((mine, theirs, expectedMine, expectedTheirs) in listOf(
        listOf(15, 20, 20, 15), listOf(20, 15, 15, 20), listOf(16, 20, 20, 16),
        listOf(14, 20, 14, 20), listOf(20, 14, 20, 14), listOf(20, 20, 20, 20),
    )) {
        test("life totals $mine and $theirs resolve to $expectedMine and $expectedTheirs") {
            val driver = newGame()
            val you = driver.activePlayer!!
            val opponent = driver.state.turnOrder.first { it != you }
            driver.setLifeTotal(you, mine)
            driver.setLifeTotal(opponent, theirs)
            driver.giveMana(you, Color.BLUE, 5)
            val card = driver.putCardInHand(you, "Psychic Transfer")
            driver.castSpell(you, card, listOf(opponent)).error shouldBe null
            driver.bothPass()
            driver.getLifeTotal(you) shouldBe expectedMine
            driver.getLifeTotal(opponent) shouldBe expectedTheirs
            (card in driver.state.getGraveyard(you)) shouldBe true
        }
    }
    test("difference is checked on resolution after life totals change") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        driver.setLifeTotal(you, 10)
        driver.giveMana(you, Color.BLUE, 5)
        val card = driver.putCardInHand(you, "Psychic Transfer")
        driver.castSpell(you, card, listOf(opponent)).error shouldBe null
        driver.setLifeTotal(you, 15)
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 20
        driver.getLifeTotal(opponent) shouldBe 15
    }
    test("self is a legal target and exchanges nothing") {
        val driver = newGame()
        val you = driver.activePlayer!!
        driver.giveMana(you, Color.BLUE, 5)
        val card = driver.putCardInHand(you, "Psychic Transfer")
        driver.castSpell(you, card, listOf(you)).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 20
        (card in driver.state.getGraveyard(you)) shouldBe true
    }
    test("a gain prohibition cancels the exchange") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        driver.setLifeTotal(you, 15)
        driver.addComponent(you, CantGainLifeComponent())
        driver.giveMana(you, Color.BLUE, 5)
        val card = driver.putCardInHand(you, "Psychic Transfer")
        driver.castSpell(you, card, listOf(opponent)).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 15
        driver.getLifeTotal(opponent) shouldBe 20
    }
    test("negative life totals are exchanged using their actual difference") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        driver.putPermanentOnBattlefield(you, "Platinum Angel")
        driver.putPermanentOnBattlefield(opponent, "Platinum Angel")
        driver.setLifeTotal(opponent, 3)
        driver.giveMana(you, Color.BLUE, 5)
        val card = driver.putCardInHand(you, "Psychic Transfer")
        driver.castSpell(you, card, listOf(opponent)).error shouldBe null
        driver.setLifeTotal(you, -2) // The total changes while the spell is on the stack.
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 3
        driver.getLifeTotal(opponent) shouldBe -2
    }
    test("an initially close target may move outside the range before resolution") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        driver.setLifeTotal(you, 15)
        driver.giveMana(you, Color.BLUE, 5)
        val card = driver.putCardInHand(you, "Psychic Transfer")
        driver.castSpell(you, card, listOf(opponent)).error shouldBe null
        driver.setLifeTotal(you, 14)
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 14
        driver.getLifeTotal(opponent) shouldBe 20
    }

})
