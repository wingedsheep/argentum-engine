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

class NearheathStalkerScenarioTest : FunSpec({

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

    test("undying: it returns as a 5/2 with a +1/+1 counter") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val stalker = driver.putCreatureOnBattlefield(you, "Nearheath Stalker")
        driver.bolt(you, stalker)

        val returned = driver.findPermanent(you, "Nearheath Stalker")
        returned shouldNotBe null
        driver.plusOnes(returned!!) shouldBe 1
        driver.state.projectedState.getPower(returned) shouldBe 5
        driver.state.projectedState.getToughness(returned) shouldBe 2
    }
})
