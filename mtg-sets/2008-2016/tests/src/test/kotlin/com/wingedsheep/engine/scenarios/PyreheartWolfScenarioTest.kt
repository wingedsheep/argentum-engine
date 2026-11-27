package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.sdk.core.Keyword

class PyreheartWolfScenarioTest : FunSpec({

    fun GameTestDriver.bolt(you: EntityId, target: EntityId) {
        val bolt = putCardInHand(you, "Lightning Bolt")
        giveMana(you, Color.RED, 1)
        castSpell(you, bolt, listOf(target)).outcome shouldBe Outcome.Done
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("when it attacks, creatures you control gain menace") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val wolf = driver.putCreatureOnBattlefield(you, "Pyreheart Wolf")
        driver.removeSummoningSickness(wolf)
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(wolf), opponent).error shouldBe null
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.state.projectedState.hasKeyword(wolf, Keyword.MENACE) shouldBe true
        driver.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
        driver.state.projectedState.hasKeyword(theirBears, Keyword.MENACE) shouldBe false
    }

    test("undying: it returns with a +1/+1 counter") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val wolf = driver.putCreatureOnBattlefield(you, "Pyreheart Wolf")
        driver.bolt(you, wolf)

        val returned = driver.findPermanent(you, "Pyreheart Wolf")
        returned shouldNotBe null
        driver.plusOnes(returned!!) shouldBe 1
    }
})
