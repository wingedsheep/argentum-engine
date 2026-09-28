package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ptk.cards.DongZhouTheTyrant
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DongZhouTheTyrantScenarioTest : FunSpec({
    test("ETB has the opponent's creature deal damage equal to its power to its controller") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all)
            registerCard(DongZhouTheTyrant)
        }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val giant = driver.putCreatureOnBattlefield(opp, "Hill Giant")
        val dong = driver.putCardInHand(me, "Dong Zhou, the Tyrant")
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.RED, 5)

        driver.castSpell(me, dong).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.submitTargetSelection(me, listOf(giant)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe 17
        driver.getLifeTotal(me) shouldBe 20
    }
})
