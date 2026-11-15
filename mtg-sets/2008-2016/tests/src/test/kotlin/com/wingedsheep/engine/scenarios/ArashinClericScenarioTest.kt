package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.ArashinCleric
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Arashin Cleric (Fate Reforged #5) — {1}{W} Creature — Human Cleric 1/3.
 *
 * "When this creature enters, you gain 3 life."
 */
class ArashinClericScenarioTest : FunSpec({

    test("entering gains its controller 3 life") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ArashinCleric)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val cleric = driver.putCardInHand(me, "Arashin Cleric")
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, cleric).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.findPermanent(me, "Arashin Cleric") shouldNotBe null
        driver.getLifeTotal(me) shouldBe 23
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
