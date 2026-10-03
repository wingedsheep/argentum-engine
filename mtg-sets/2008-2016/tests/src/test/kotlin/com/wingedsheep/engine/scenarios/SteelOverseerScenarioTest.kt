package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m11.cards.SteelOverseer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Steel Overseer (M11 #214) — {2} Artifact Creature — Construct 1/1
 * "{T}: Put a +1/+1 counter on each artifact creature you control."
 *
 * The counter lands on every artifact creature its controller controls (itself included), and
 * skips both non-artifact creatures and the opponent's artifact creatures.
 */
class SteelOverseerScenarioTest : FunSpec({

    val tapAbilityId = SteelOverseer.activatedAbilities.single().id

    test("{T}: +1/+1 counter on each artifact creature you control, and only those") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SteelOverseer)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val overseer = driver.putCreatureOnBattlefield(me, "Steel Overseer")
        driver.removeSummoningSickness(overseer)
        val myGolem = driver.putCreatureOnBattlefield(me, "Artifact Creature")
        val myLions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val theirGolem = driver.putCreatureOnBattlefield(opponent, "Artifact Creature")

        driver.submit(
            ActivateAbility(playerId = me, sourceId = overseer, abilityId = tapAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        fun plusOnes(id: EntityId): Int =
            driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        plusOnes(overseer) shouldBe 1
        plusOnes(myGolem) shouldBe 1
        plusOnes(myLions) shouldBe 0
        plusOnes(theirGolem) shouldBe 0
        driver.isTapped(overseer) shouldBe true
        driver.state.projectedState.getPower(overseer) shouldBe 2
        driver.state.projectedState.getPower(myGolem) shouldBe 3
    }
})
