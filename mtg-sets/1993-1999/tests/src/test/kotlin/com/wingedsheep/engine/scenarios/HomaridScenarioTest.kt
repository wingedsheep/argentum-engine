package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.Homarid
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Homarid (Fallen Empires).
 *
 * The tide is what makes this card unusual: both statics compare the counter count for *equality*,
 * not for a threshold, so a Homarid at three counters is a 3/3 and is emphatically not also "at
 * least one" and therefore -1/-1 as well. These tests walk the four-beat cycle a counter at a time.
 */
class HomaridScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Homarid)
        return driver
    }

    fun withTide(driver: GameTestDriver, homarid: com.wingedsheep.sdk.model.EntityId, count: Int) {
        driver.replaceState(
            driver.state.updateEntity(homarid) { c ->
                c.with(CountersComponent(mapOf(CounterType.TIDE to count)))
            }
        )
    }

    test("the tide cycle: 1 -> 1/1, 2 -> 2/2, 3 -> 3/3") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val homarid = driver.putCreatureOnBattlefield(alice, "Homarid")

        withTide(driver, homarid, 1)
        withClue("exactly one tide counter is -1/-1") {
            projector.project(driver.state).getPower(homarid) shouldBe 1
            projector.project(driver.state).getToughness(homarid) shouldBe 1
        }

        withTide(driver, homarid, 2)
        withClue("two counters match neither static") {
            projector.project(driver.state).getPower(homarid) shouldBe 2
            projector.project(driver.state).getToughness(homarid) shouldBe 2
        }

        withTide(driver, homarid, 3)
        withClue("exactly three tide counters is +1/+1 — and not also the -1/-1") {
            projector.project(driver.state).getPower(homarid) shouldBe 3
            projector.project(driver.state).getToughness(homarid) shouldBe 3
        }
    }

    test("a fourth counter is shed and the tide restarts") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val homarid = driver.putCreatureOnBattlefield(alice, "Homarid")

        withTide(driver, homarid, 4)
        driver.passPriorityUntil(Step.END)

        withClue("the state trigger emptied the tide") {
            (driver.state.getEntity(homarid)?.get<CountersComponent>()?.getCount(CounterType.TIDE) ?: 0) shouldBe 0
        }
        withClue("back to a plain 2/2") {
            projector.project(driver.state).getPower(homarid) shouldBe 2
            projector.project(driver.state).getToughness(homarid) shouldBe 2
        }
    }
})
