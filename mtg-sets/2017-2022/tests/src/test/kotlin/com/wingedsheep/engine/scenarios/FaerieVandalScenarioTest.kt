package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.Divination
import com.wingedsheep.mtg.sets.definitions.eld.cards.FaerieVandal
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Faerie Vandal (ELD #45)
 * {1}{U} Creature — Faerie Rogue 1/2
 * Flash, Flying
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 */
class FaerieVandalScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FaerieVandal, Divination))
        return driver
    }

    fun plusOnes(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("drawing the second card of the turn puts exactly one +1/+1 counter on it") {
        // Turn-1 active player skipped the draw step, so Divination draws cards 1 and 2.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Divination" to 10))

        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val vandal = driver.putCreatureOnBattlefield(p1, "Faerie Vandal")

        val divination = driver.putCardInHand(p1, "Divination")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, divination)
        driver.bothPass()
        driver.bothPass()

        plusOnes(driver, vandal) shouldBe 1

        // A third and fourth draw the same turn don't trigger it again.
        val second = driver.putCardInHand(p1, "Divination")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, second)
        driver.bothPass()
        driver.bothPass()

        plusOnes(driver, vandal) shouldBe 1
    }

    test("an opponent's second draw does not trigger it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Divination" to 10))

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val vandal = driver.putCreatureOnBattlefield(p1, "Faerie Vandal")

        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val divination = driver.putCardInHand(p2, "Divination")
        driver.giveMana(p2, Color.BLUE, 1)
        driver.giveColorlessMana(p2, 2)
        driver.castSpell(p2, divination)
        driver.bothPass()
        driver.bothPass()

        plusOnes(driver, vandal) shouldBe 0
    }
})
