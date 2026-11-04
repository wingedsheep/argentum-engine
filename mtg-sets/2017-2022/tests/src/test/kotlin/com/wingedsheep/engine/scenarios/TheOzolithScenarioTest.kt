package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.TheOzolith
import com.wingedsheep.mtg.sets.definitions.lea.cards.Unsummon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Ozolith (IKO #237) — {1} Legendary Artifact.
 *
 * "Whenever a creature you control leaves the battlefield, if it had counters on it, put those
 * counters on The Ozolith. At the beginning of combat on your turn, if The Ozolith has counters on
 * it, you may move all counters from The Ozolith onto target creature."
 *
 * Pins the leaves-not-dies trigger (a bounce counts), every counter kind riding along, the
 * intervening-if on both abilities, and the all-or-nothing "you may" move.
 */
class TheOzolithScenarioTest : FunSpec({

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(TheOzolith, Unsummon))
        initMirrorMatch(Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.setCounters(id: EntityId, counters: Map<CounterType, Int>) {
        replaceState(state.updateEntity(id) { it.with(CountersComponent(counters)) })
    }

    fun GameTestDriver.counters(id: EntityId): Map<CounterType, Int> =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.filterValues { it > 0 }.orEmpty()

    test("a bounced creature's counters, every kind, go onto The Ozolith") {
        val d = setup()
        val me = d.activePlayer!!
        val ozolith = d.putPermanentOnBattlefield(me, "The Ozolith")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.setCounters(bear, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.FLYING to 1))
        d.giveMana(me, Color.BLUE, 1)

        d.castSpell(me, d.putCardInHand(me, "Unsummon"), targets = listOf(bear))
        d.bothPass() // Unsummon resolves; the leaves trigger goes on the stack
        d.bothPass() // the trigger resolves

        d.counters(ozolith) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.FLYING to 1)
    }

    test("a creature that leaves with no counters doesn't trigger") {
        val d = setup()
        val me = d.activePlayer!!
        val ozolith = d.putPermanentOnBattlefield(me, "The Ozolith")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.giveMana(me, Color.BLUE, 1)

        d.castSpell(me, d.putCardInHand(me, "Unsummon"), targets = listOf(bear))
        d.bothPass()

        d.stackSize shouldBe 0
        d.counters(ozolith) shouldBe emptyMap()
    }

    test("at beginning of combat, accepting moves all counters onto the target creature") {
        val d = setup()
        val me = d.activePlayer!!
        val ozolith = d.putPermanentOnBattlefield(me, "The Ozolith")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.setCounters(ozolith, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3, CounterType.FLYING to 1))

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(me, listOf(bear))
        d.bothPass()
        (d.pendingDecision is YesNoDecision) shouldBe true
        d.submitYesNo(me, true)

        d.counters(ozolith) shouldBe emptyMap()
        d.counters(bear) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3, CounterType.FLYING to 1)
        d.state.projectedState.getPower(bear) shouldBe 5
    }

    test("declining the move leaves every counter on The Ozolith") {
        val d = setup()
        val me = d.activePlayer!!
        val ozolith = d.putPermanentOnBattlefield(me, "The Ozolith")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.setCounters(ozolith, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3))

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.submitTargetSelection(me, listOf(bear))
        d.bothPass()
        d.submitYesNo(me, false)

        d.counters(ozolith) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)
        d.counters(bear) shouldBe emptyMap()
    }

    test("an Ozolith with no counters doesn't trigger at beginning of combat") {
        val d = setup()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "The Ozolith")
        d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.passPriorityUntil(Step.BEGIN_COMBAT)

        d.pendingDecision shouldBe null
        d.stackSize shouldBe 0
    }
})
