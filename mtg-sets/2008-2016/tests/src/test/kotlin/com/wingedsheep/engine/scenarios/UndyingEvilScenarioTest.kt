package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class UndyingEvilScenarioTest : FunSpec({

    test("the targeted creature gains undying and returns with a +1/+1 counter when it dies") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val evil = driver.putCardInHand(you, "Undying Evil")
        driver.giveMana(you, Color.BLACK, 1)
        driver.castSpell(you, evil, listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.projectedState.hasKeyword(bears, Keyword.UNDYING) shouldBe true

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(bears)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        val returned = driver.findPermanent(you, "Grizzly Bears")
        returned shouldNotBe null
        driver.state.getEntity(returned!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
})
