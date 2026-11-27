package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ErdwalRipperScenarioTest : FunSpec({

    test("haste, and combat damage to a player puts a +1/+1 counter on it") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val ripper = driver.putCreatureOnBattlefield(you, "Erdwal Ripper")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(ripper), opponent).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.getLifeTotal(opponent) shouldBe 18
        driver.state.getEntity(ripper)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.state.projectedState.getPower(ripper) shouldBe 3
    }
})
