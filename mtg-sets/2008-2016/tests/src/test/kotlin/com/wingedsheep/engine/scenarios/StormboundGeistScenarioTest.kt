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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class StormboundGeistScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("it can block only creatures with flying") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val birds = driver.putCreatureOnBattlefield(you, "Birds of Paradise")
        driver.removeSummoningSickness(bears)
        driver.removeSummoningSickness(birds)
        val geist = driver.putCreatureOnBattlefield(opponent, "Stormbound Geist")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(bears, birds), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(opponent, mapOf(geist to listOf(bears))).outcome shouldNotBe Outcome.Done
        driver.declareBlockers(opponent, mapOf(geist to listOf(birds))).outcome shouldBe Outcome.Done
    }

    test("undying returns it with a +1/+1 counter") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val geist = driver.putCreatureOnBattlefield(you, "Stormbound Geist")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(geist)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        val returned = driver.findPermanent(you, "Stormbound Geist")
        returned shouldNotBe null
        driver.state.getEntity(returned!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
})
