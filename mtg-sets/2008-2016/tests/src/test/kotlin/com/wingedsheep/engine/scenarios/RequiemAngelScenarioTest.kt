package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RequiemAngelScenarioTest : FunSpec({

    fun boltOwnCreature(victimName: String): Pair<GameTestDriver, List<com.wingedsheep.sdk.model.EntityId>> {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val angel = driver.putCreatureOnBattlefield(you, "Requiem Angel")
        val victim = driver.putCreatureOnBattlefield(you, victimName)

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(victim)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.getGraveyard(you).contains(victim) shouldBe true
        return driver to driver.getCreatures(you).filter { it != angel }
    }

    test("another non-Spirit creature you control dying creates a 1/1 flying Spirit") {
        val (driver, others) = boltOwnCreature("Grizzly Bears")
        val token = others.single()
        driver.state.projectedState.getPower(token) shouldBe 1
        driver.state.projectedState.hasKeyword(token, Keyword.FLYING) shouldBe true
    }

    test("a Spirit creature dying creates nothing") {
        val (_, others) = boltOwnCreature("Niblis of the Mist")
        others.size shouldBe 0
    }
})
