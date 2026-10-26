package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.som.cards.IchorRats
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ichor Rats: Infect. When this creature enters, each player gets a poison counter.
 * Pins the symmetric ETB (its controller is poisoned too) and that its combat damage to a player
 * is poison, not life loss.
 */
class IchorRatsScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(IchorRats)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.poison(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("entering gives each player, its controller included, a poison counter") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val rats = driver.putCardInHand(me, "Ichor Rats")
        driver.giveMana(me, Color.BLACK, 3)

        driver.castSpell(me, rats)
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the enters trigger

        driver.poison(me) shouldBe 1
        driver.poison(opponent) shouldBe 1
    }

    test("its combat damage to a player is dealt as poison counters") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val rats = driver.putCreatureOnBattlefield(me, "Ichor Rats")
        driver.removeSummoningSickness(rats)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(rats), opponent)
        driver.bothPass()
        driver.declareNoBlockers(opponent)
        driver.bothPass()
        driver.bothPass()

        driver.poison(opponent) shouldBe 2
        driver.assertLifeTotal(opponent, 20)
    }
})
