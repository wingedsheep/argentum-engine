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

class GravetillerWurmScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castWurm(you: EntityId): EntityId {
        val wurm = putCardInHand(you, "Gravetiller Wurm")
        giveMana(you, Color.GREEN, 6)
        castSpell(you, wurm).outcome shouldBe Outcome.Done
        bothPass()
        return findPermanent(you, "Gravetiller Wurm")!!
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("morbid: it enters with four +1/+1 counters after a creature died this turn") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.getGraveyard(opponent).contains(bears) shouldBe true

        val wurm = driver.castWurm(you)
        driver.plusOnes(wurm) shouldBe 4
        driver.state.projectedState.getPower(wurm) shouldBe 8
    }

    test("without a death this turn it enters with no counters") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val wurm = driver.castWurm(you)
        driver.plusOnes(wurm) shouldBe 0
        driver.state.projectedState.getPower(wurm) shouldBe 4
    }
})
