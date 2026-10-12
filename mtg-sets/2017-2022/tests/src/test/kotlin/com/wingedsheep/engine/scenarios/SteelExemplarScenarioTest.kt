package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.SteelExemplar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Steel Exemplar — "enters with two +1/+1 counters on it unless two or more colors of mana were
 * spent to cast it." The condition counts distinct colors (not pips) of the entering creature's
 * own cast; colorless mana is not a color.
 */
class SteelExemplarScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(SteelExemplar))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("all colorless mana spent: enters with two +1/+1 counters") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Steel Exemplar")
        driver.giveColorlessMana(p, 5)

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        plusCounters(driver, spell) shouldBe 2
    }

    test("one color spent (five green): still enters with two +1/+1 counters") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Steel Exemplar")
        driver.giveMana(p, Color.GREEN, 5)

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        plusCounters(driver, spell) shouldBe 2
    }

    test("two colors spent: enters with no counters") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Steel Exemplar")
        driver.giveColorlessMana(p, 3)
        driver.giveMana(p, Color.WHITE, 1)
        driver.giveMana(p, Color.BLUE, 1)

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        plusCounters(driver, spell) shouldBe 0
    }
})
