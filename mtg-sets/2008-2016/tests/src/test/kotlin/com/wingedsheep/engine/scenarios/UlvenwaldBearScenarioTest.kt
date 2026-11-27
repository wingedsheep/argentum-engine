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
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UlvenwaldBearScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castBear(you: EntityId, target: EntityId) {
        val bear = putCardInHand(you, "Ulvenwald Bear")
        giveMana(you, Color.GREEN, 3)
        castSpell(you, bear).outcome shouldBe Outcome.Done
        var guard = 0
        while (guard++ < 30 && (pendingDecision != null || state.stack.isNotEmpty())) {
            when (pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(you, listOf(target))
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("morbid: after a creature died this turn, it puts two +1/+1 counters on target creature") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(victim)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.getGraveyard(opponent).contains(victim) shouldBe true

        driver.castBear(you, courser)
        driver.plusOnes(courser) shouldBe 2
        driver.state.projectedState.getPower(courser) shouldBe 5
    }

    test("without a death this turn the trigger does nothing") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")

        driver.castBear(you, courser)
        driver.plusOnes(courser) shouldBe 0
    }
})
