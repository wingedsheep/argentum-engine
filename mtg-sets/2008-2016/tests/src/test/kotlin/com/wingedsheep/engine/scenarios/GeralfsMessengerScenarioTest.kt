package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GeralfsMessengerScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll(you: com.wingedsheep.sdk.model.EntityId) {
        val opponent = getOpponent(you)
        var guard = 0
        while (guard++ < 30) {
            when (pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(you, listOf(opponent))
                else -> if (state.stack.isNotEmpty()) bothPass() else break
            }
        }
    }

    test("it enters tapped and the target opponent loses 2 life") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val messenger = driver.putCardInHand(you, "Geralf's Messenger")
        driver.giveMana(you, Color.BLACK, 3)
        driver.castSpell(you, messenger).outcome shouldBe Outcome.Done
        driver.resolveAll(you)

        val onField = driver.findPermanent(you, "Geralf's Messenger")
        onField shouldNotBe null
        driver.isTapped(onField!!) shouldBe true
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("undying returns it with a +1/+1 counter and drains again") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val messenger = driver.putCreatureOnBattlefield(you, "Geralf's Messenger")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(messenger)).outcome shouldBe Outcome.Done
        driver.resolveAll(you)

        val returned = driver.findPermanent(you, "Geralf's Messenger")
        returned shouldNotBe null
        driver.state.getEntity(returned!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.isTapped(returned) shouldBe true
        driver.getLifeTotal(opponent) shouldBe 18
    }
})
