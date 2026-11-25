package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.BondBeetle
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bond Beetle {G} — Creature — Insect 0/1
 *   When this creature enters, put a +1/+1 counter on target creature.
 */
class BondBeetleScenarioTest : FunSpec({

    test("entering puts a +1/+1 counter on the targeted creature") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BondBeetle))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val beetle = driver.putCardInHand(me, "Bond Beetle")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, beetle)

        var guard = 0
        while (guard++ < 20 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            when (driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(me, listOf(bears))
                null -> driver.bothPass()
                else -> driver.autoResolveDecision()
            }
        }

        driver.state.getEntity(bears)!!.get<CountersComponent>()!!
            .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.state.projectedState.getPower(bears) shouldBe 4
    }
})
