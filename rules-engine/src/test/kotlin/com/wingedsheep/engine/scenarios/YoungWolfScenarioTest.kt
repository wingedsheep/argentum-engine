package com.wingedsheep.engine.scenarios

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

class YoungWolfScenarioTest : FunSpec({

    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun plusOneCounters(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun killWithLightningBolt(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId, victim: com.wingedsheep.sdk.model.EntityId) {
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, listOf(victim)).isSuccess shouldBe true
        driver.bothPass()
    }

    test("undying returns Young Wolf with a +1/+1 counter and does not trigger on its next death") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val wolf = driver.putCreatureOnBattlefield(you, "Young Wolf")

        killWithLightningBolt(driver, you, wolf)
        driver.state.getGraveyard(you).contains(wolf) shouldBe true
        driver.bothPass()

        driver.state.getBattlefield().contains(wolf) shouldBe true
        plusOneCounters(driver, wolf) shouldBe 1

        killWithLightningBolt(driver, you, wolf)

        driver.state.getGraveyard(you).contains(wolf) shouldBe true
        driver.state.stack.isEmpty() shouldBe true
    }
})
