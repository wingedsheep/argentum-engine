package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.GitaxianRaptor
import com.wingedsheep.mtg.sets.definitions.one.cards.IchorplateGolem
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ichorplate Golem (ONE #230) — {3} 2/3 Artifact Creature — Phyrexian Golem.
 *
 * "Whenever a creature you control enters, if it has one or more oil counters on it, put an oil
 *  counter on it. Creatures you control with oil counters on them get +1/+1."
 */
class IchorplateGolemScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IchorplateGolem, GitaxianRaptor))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    test("a creature entering with oil counters gets another, and the lord pumps it") {
        val driver = newDriver()
        val p1 = driver.player1
        val golem = driver.putCreatureOnBattlefield(p1, "Ichorplate Golem")
        val raptor = driver.putCardInHand(p1, "Gitaxian Raptor")
        driver.giveMana(p1, Color.BLUE, 3)
        driver.castSpell(p1, raptor).error shouldBe null
        driver.bothPass() // Raptor resolves, enters with three oil counters
        driver.stackSize shouldBe 1
        driver.bothPass() // Golem trigger resolves

        oil(driver, raptor) shouldBe 4
        driver.state.projectedState.getPower(raptor) shouldBe 2
        driver.state.projectedState.getToughness(raptor) shouldBe 5
        // The Golem itself has no oil counter, so it isn't pumped.
        driver.state.projectedState.getPower(golem) shouldBe 2
        driver.state.projectedState.getToughness(golem) shouldBe 3
    }

    test("a creature entering without oil counters doesn't trigger and isn't pumped") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.putCreatureOnBattlefield(p1, "Ichorplate Golem")
        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.castSpell(p1, bears).error shouldBe null
        driver.bothPass() // Bears resolve

        driver.stackSize shouldBe 0
        oil(driver, bears) shouldBe 0
        driver.state.projectedState.getPower(bears) shouldBe 2
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }

    test("an opponent's creature entering with oil counters is neither triggered on nor pumped") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Ichorplate Golem")
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.activePlayerId shouldBe driver.player2

        val p2 = driver.player2
        val raptor = driver.putCardInHand(p2, "Gitaxian Raptor")
        driver.giveMana(p2, Color.BLUE, 3)
        driver.castSpell(p2, raptor).error shouldBe null
        driver.bothPass() // Raptor resolves, enters with three oil counters

        driver.stackSize shouldBe 0
        oil(driver, raptor) shouldBe 3
        driver.state.projectedState.getPower(raptor) shouldBe 1
        driver.state.projectedState.getToughness(raptor) shouldBe 4
    }

    test("a creature you control that gains an oil counter later is pumped") {
        val driver = newDriver()
        val p1 = driver.player1
        val golem = driver.putCreatureOnBattlefield(p1, "Ichorplate Golem")
        driver.addComponent(golem, CountersComponent(mapOf(CounterType.OIL to 1)))

        driver.state.projectedState.getPower(golem) shouldBe 3
        driver.state.projectedState.getToughness(golem) shouldBe 4
    }
})
