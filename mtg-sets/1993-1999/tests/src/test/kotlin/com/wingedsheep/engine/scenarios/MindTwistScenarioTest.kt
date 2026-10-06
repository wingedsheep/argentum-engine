package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.MindTwist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mind Twist — "Target player discards X cards at random."
 *
 * The cast-time X has to reach the random selection inside the resolution pipeline: X=2 must
 * discard exactly two, and an X larger than the hand discards the whole hand rather than failing.
 */
class MindTwistScenarioTest : FunSpec({

    fun cast(x: Int): Triple<GameTestDriver, Int, Int> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MindTwist)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val twist = driver.putCardInHand(me, "Mind Twist")
        driver.giveMana(me, Color.BLACK, x + 1)
        val handBefore = driver.getHandSize(opponent)

        driver.castXSpell(me, twist, xValue = x, targets = listOf(opponent)).error shouldBe null
        driver.bothPass()

        return Triple(driver, handBefore, driver.getHandSize(opponent))
    }

    test("X = 2 discards exactly two cards at random") {
        val (driver, before, after) = cast(2)
        val opponent = driver.getOpponent(driver.activePlayer!!)
        withClue("two cards left the hand") { after shouldBe before - 2 }
        withClue("both went to the graveyard") { driver.getGraveyardCardNames(opponent).size shouldBe 2 }
    }

    test("X larger than the hand discards the whole hand") {
        val (driver, before, after) = cast(10)
        val opponent = driver.getOpponent(driver.activePlayer!!)
        after shouldBe 0
        driver.getGraveyardCardNames(opponent).size shouldBe before
    }
})
