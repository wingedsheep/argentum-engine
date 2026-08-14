package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.AvacynRestoredSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ButcherGhoulScenarioTest : FunSpec({

    test("a +1/+1 counter present as Butcher Ghoul dies prevents undying from triggering") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + AvacynRestoredSet.cards)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val ghoul = driver.putCreatureOnBattlefield(you, "Butcher Ghoul")
        driver.addComponent(
            ghoul,
            CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
        )

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(ghoul)).isSuccess shouldBe true
        driver.bothPass()

        driver.state.getGraveyard(you).contains(ghoul) shouldBe true
        driver.state.stack.isEmpty() shouldBe true
    }
})
