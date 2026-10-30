package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SuspendCardFromHand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.AncestralVision
import com.wingedsheep.mtg.sets.definitions.tsp.cards.Clockspinning
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Clockspinning — {U} Instant (Time Spiral).
 *
 * "Buyback {3}. Choose a counter on target permanent or suspended card. Remove that counter from
 * that permanent or card or put another of those counters on it."
 *
 * Covers the two halves only this card combines: a suspended card in exile as a legal target
 * (CR 702.62b), and taking its last time counter off firing suspend's cast trigger on the spot
 * ("it doesn't matter why the last time counter was removed").
 */
class ClockspinningScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Clockspinning, AncestralVision))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver
    }

    fun counters(driver: GameTestDriver, id: EntityId, type: CounterType): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun chooseAction(driver: GameTestDriver, player: EntityId, labelStart: String) {
        val decision = driver.state.pendingDecision as ChooseOptionDecision
        val index = decision.options.indexOfFirst { it.startsWith(labelStart) }
        driver.submitDecision(player, OptionChosenResponse(decision.id, index))
    }

    test("removing a suspended card's last time counter lets its owner cast it right away") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vision = driver.putCardInHand(me, "Ancestral Vision")
        driver.giveMana(me, Color.BLUE, 1)
        driver.submitSuccess(SuspendCardFromHand(me, vision))
        // Fast-forward the countdown to its last counter.
        driver.replaceState(
            driver.state.updateEntity(vision) {
                it.with(CountersComponent().withAdded(CounterType.TIME, 1))
            }
        )

        val spin = driver.putCardInHand(me, "Clockspinning")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpellWithTargets(me, spin, listOf(ChosenTarget.Card(vision, me, Zone.EXILE)))
            .outcome shouldBe Outcome.Done
        driver.bothPass()
        chooseAction(driver, me, "Remove")
        counters(driver, vision, CounterType.TIME) shouldBe 0
        driver.getGraveyard(me).contains(spin) shouldBe true // no buyback paid

        // Suspend's cast trigger is on the stack now — no waiting for the next upkeep.
        driver.state.stack.size shouldBe 1
        driver.state.getEntity(driver.state.stack.single())
            ?.get<TriggeredAbilityOnStackComponent>()?.sourceId shouldBe vision
        val handBefore = driver.getHand(me).size
        driver.bothPass()
        driver.submitYesNo(me, true)
        // Ancestral Vision's target ("target player draws three cards") is chosen as it's cast.
        driver.submitTargetSelection(me, listOf(me))
        driver.bothPass()

        driver.getExile(me).contains(vision) shouldBe false
        driver.getHand(me).size shouldBe handBefore + 3
    }

    test("putting another time counter on a suspended card delays it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vision = driver.putCardInHand(me, "Ancestral Vision")
        driver.giveMana(me, Color.BLUE, 1)
        driver.submitSuccess(SuspendCardFromHand(me, vision))

        val spin = driver.putCardInHand(me, "Clockspinning")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpellWithTargets(me, spin, listOf(ChosenTarget.Card(vision, me, Zone.EXILE)))
            .outcome shouldBe Outcome.Done
        driver.bothPass()
        chooseAction(driver, me, "Put another")

        counters(driver, vision, CounterType.TIME) shouldBe 5
        driver.state.stack.isEmpty() shouldBe true
    }

    test("a permanent's counter can be added to as well") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.replaceState(
            driver.state.updateEntity(bears) {
                it.with(CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, 1))
            }
        )

        val spin = driver.putCardInHand(me, "Clockspinning")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, spin, listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()
        chooseAction(driver, me, "Put another")

        // 2/2 with two -1/-1 counters dies to state-based actions.
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
    }
})
