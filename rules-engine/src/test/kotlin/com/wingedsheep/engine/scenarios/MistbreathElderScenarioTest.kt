package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.MistbreathElder
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mistbreath Elder (BLB) — "At the beginning of your upkeep, return another creature
 * you control to its owner's hand. If you do, put a +1/+1 counter on this creature.
 * Otherwise, you may return this creature to its owner's hand."
 *
 * The forced bounce is authored as a Gather → Select → Move pipeline (replacing the
 * former bespoke ForceReturnOwnPermanentEffect): the battlefield→hand move routes to
 * the owner's hand, and a single candidate is auto-selected (forced choice).
 */
class MistbreathElderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MistbreathElder))
        return driver
    }

    // Advance to the next upkeep belonging to [player] (skips one turn if we're on the other player's).
    fun advanceToUpkeepOf(driver: GameTestDriver, player: EntityId) {
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        if (driver.activePlayer != player) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe player
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("with another creature: it is returned to its owner's hand and the Elder gets a counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val elder = driver.putCreatureOnBattlefield(controller, "Mistbreath Elder")
        val bear = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        advanceToUpkeepOf(driver, controller)
        driver.stackSize shouldBe 1
        driver.bothPass() // single candidate — the forced selection is made automatically

        driver.findPermanent(controller, "Grizzly Bears") shouldBe null
        driver.getHand(controller).contains(bear) shouldBe true
        plusOneCounters(driver, elder) shouldBe 1
    }

    test("with two candidates: the controller chooses which creature to bounce") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val elder = driver.putCreatureOnBattlefield(controller, "Mistbreath Elder")
        val bear1 = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")
        val bear2 = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        advanceToUpkeepOf(driver, controller)
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.state.pendingDecision shouldNotBe null
        driver.submitCardSelection(controller, listOf(bear2))

        driver.getHand(controller).contains(bear2) shouldBe true
        driver.findPermanent(controller, "Grizzly Bears") shouldBe bear1
        plusOneCounters(driver, elder) shouldBe 1
    }

    test("alone: may return itself — accepting bounces the Elder with no counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val elder = driver.putCreatureOnBattlefield(controller, "Mistbreath Elder")

        advanceToUpkeepOf(driver, controller)
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.state.pendingDecision shouldNotBe null
        driver.submitYesNo(controller, true)

        driver.findPermanent(controller, "Mistbreath Elder") shouldBe null
        driver.getHand(controller).contains(elder) shouldBe true
    }

    test("alone: declining keeps the Elder on the battlefield with no counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val elder = driver.putCreatureOnBattlefield(controller, "Mistbreath Elder")

        advanceToUpkeepOf(driver, controller)
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.state.pendingDecision shouldNotBe null
        driver.submitYesNo(controller, false)

        driver.findPermanent(controller, "Mistbreath Elder") shouldBe elder
        plusOneCounters(driver, elder) shouldBe 0
    }
})
