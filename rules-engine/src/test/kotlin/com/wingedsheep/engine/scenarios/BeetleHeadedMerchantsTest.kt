package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.BeetleHeadedMerchants
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Beetle-Headed Merchants: "Whenever this creature attacks, you may sacrifice another creature or
 * artifact. If you do, draw a card and put a +1/+1 counter on this creature."
 *
 * Modeled as a non-targeted [ReflexiveTriggerEffect]: the optional action is a `SacrificeEffect`
 * (another creature/artifact you control), and the draw + counter payoff fires only when a
 * sacrifice actually happens ("If you do"). These tests pin both branches of the choice.
 */
class BeetleHeadedMerchantsTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BeetleHeadedMerchants))
        return driver
    }

    fun plusCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("choosing to sacrifice draws a card and puts a +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val beetle = driver.putCreatureOnBattlefield(me, "Beetle-Headed Merchants")
        driver.removeSummoningSickness(beetle)
        driver.putCreatureOnBattlefield(me, "Savannah Lions")   // the sacrifice fodder
        val handBefore = driver.getHandSize(me)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(beetle), opponent)
        driver.bothPass()                 // resolve the attack trigger → may yes/no
        driver.submitYesNo(me, true)      // opt to sacrifice; one valid fodder → auto-sacrificed
        driver.bothPass()                 // CR 603.12: reflexive payoff is a real stack object now

        plusCounters(driver, beetle) shouldBe 1
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("declining the sacrifice draws nothing and adds no counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val beetle = driver.putCreatureOnBattlefield(me, "Beetle-Headed Merchants")
        driver.removeSummoningSickness(beetle)
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val handBefore = driver.getHandSize(me)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(beetle), opponent)
        driver.bothPass()
        driver.submitYesNo(me, false)     // decline

        plusCounters(driver, beetle) shouldBe 0
        driver.getHandSize(me) shouldBe handBefore
    }
})
