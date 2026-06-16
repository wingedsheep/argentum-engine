package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.PrairieDog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.mechanics.layers.StateProjector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Prairie Dog — {1}{W} 2/2 Creature — Squirrel
 *
 * "Lifelink
 *  At the beginning of your end step, if you haven't cast a spell from your hand this turn,
 *  put a +1/+1 counter on this creature.
 *  {4}{W}: Until end of turn, if you would put one or more +1/+1 counters on a creature you control,
 *  put that many plus one +1/+1 counters on it instead."
 */
class PrairieDogScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PrairieDog)
        return driver
    }

    fun plusCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("has lifelink") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val dog = driver.putCreatureOnBattlefield(me, "Prairie Dog")
        projector.project(driver.state).hasKeyword(dog, Keyword.LIFELINK) shouldBe true
    }

    test("end step adds a +1/+1 counter when you haven't cast a spell from your hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val dog = driver.putCreatureOnBattlefield(me, "Prairie Dog")
        plusCounters(driver, dog) shouldBe 0

        // No spell cast from hand this turn -> the intervening-if holds at the end step.
        driver.passPriorityUntil(Step.END)
        driver.bothPass() // resolve the end-step trigger

        plusCounters(driver, dog) shouldBe 1
    }

    test("end step does nothing if you cast a spell from your hand this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val dog = driver.putCreatureOnBattlefield(me, "Prairie Dog")

        // Cast a spell from hand this turn -> the intervening-if fails at the end step.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))
        driver.bothPass() // resolve Bolt

        driver.passPriorityUntil(Step.END)
        driver.bothPass() // end step: intervening-if fails, no counter

        plusCounters(driver, dog) shouldBe 0
    }

    test("{4}{W} ability makes the end-step counter land as two counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val dog = driver.putCreatureOnBattlefield(me, "Prairie Dog")

        // Activate {4}{W}: until end of turn, +1 extra to +1/+1 counters on creatures you control.
        val abilityId = PrairieDog.activatedAbilities.first().id
        driver.giveMana(me, Color.WHITE, 5)
        val result = driver.submit(ActivateAbility(playerId = me, sourceId = dog, abilityId = abilityId))
        result.isSuccess shouldBe true
        driver.bothPass() // resolve the ability -> installs the modifier
        driver.state.activeCounterPlacementModifiers.size shouldBe 1

        // No spell cast from hand, so the end-step trigger puts 1 counter -> modified to 2.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        plusCounters(driver, dog) shouldBe 2
    }
})
