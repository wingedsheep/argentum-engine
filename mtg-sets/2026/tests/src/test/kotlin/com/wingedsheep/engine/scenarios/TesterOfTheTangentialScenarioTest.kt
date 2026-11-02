package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.TesterOfTheTangential
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Tester of the Tangential (SOS #69) — specifically its second ability:
 * "At the beginning of combat on your turn, you may pay {X}. When you do, move X +1/+1 counters
 * from this creature onto another target creature."
 *
 * The ability is a may-pay-{X} reflexive: the begin-combat trigger pauses on a number chooser
 * (`Gate.MayPayX`); the chosen X is paid in generic mana and bound into `DynamicAmount.XValue`,
 * which then drives `Effects.MoveCounters` after the controller selects "another target creature".
 * (Increment's own counter-growth trigger is covered by IncrementMechanicScenarioTest.)
 */
class TesterOfTheTangentialScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TesterOfTheTangential))
        return driver
    }

    fun GameTestDriver.advanceToPlayer1BeginCombat() {
        passPriorityUntil(Step.BEGIN_COMBAT)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.BEGIN_COMBAT)
            safety++
        }
    }

    fun plusCounters(driver: GameTestDriver, entityId: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("paying X=2 moves two +1/+1 counters from the Tester onto the chosen target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val tester = driver.putCreatureOnBattlefield(driver.player1, "Tester of the Tangential")
        // Two other creatures, so "another target creature" is a real choice (not auto-selected).
        val chosen = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val decoy = driver.putCreatureOnBattlefield(driver.player1, "Savannah Lions")
        driver.addComponent(tester, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))

        driver.advanceToPlayer1BeginCombat()
        // mana added here — unspent mana empties as each step/phase ends (CR 500.5); it must be
        // present for the may-pay-{X} payment during the begin-combat trigger
        driver.giveMana(driver.player1, Color.BLUE, 5)

        // BeginCombat trigger goes on the stack; resolving it presents the MayPayX number chooser.
        driver.bothPass()
        driver.submitYesNo(driver.player1, true)
        val payDecision = driver.pendingDecision
        payDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        driver.submitDecision(driver.player1, NumberChosenResponse(payDecision.id, 2))

        // After paying X=2, the reflexive effect selects "another target creature".
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(driver.player1, listOf(chosen))

        plusCounters(driver, tester) shouldBe 1   // 3 - 2
        plusCounters(driver, chosen) shouldBe 2   // 0 + 2
        plusCounters(driver, decoy) shouldBe 0
    }

    test("with exactly one other creature, the target auto-selects and X=2 moves the counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val tester = driver.putCreatureOnBattlefield(driver.player1, "Tester of the Tangential")
        val other = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.addComponent(tester, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))

        driver.advanceToPlayer1BeginCombat()
        // mana added here — unspent mana empties as each step/phase ends (CR 500.5); it must be
        // present for the may-pay-{X} payment during the begin-combat trigger
        driver.giveMana(driver.player1, Color.BLUE, 5)

        driver.bothPass()
        driver.submitYesNo(driver.player1, true)
        val payDecision = driver.pendingDecision
        payDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        driver.submitDecision(driver.player1, NumberChosenResponse(payDecision.id, 2))

        // Single "other creature" → auto-selected, no target decision.
        plusCounters(driver, tester) shouldBe 1
        plusCounters(driver, other) shouldBe 2
    }

    test("declining the payment moves no counters and asks for no target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val tester = driver.putCreatureOnBattlefield(driver.player1, "Tester of the Tangential")
        val other = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.addComponent(tester, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))

        driver.advanceToPlayer1BeginCombat()
        // mana added here — unspent mana empties as each step/phase ends (CR 500.5); it must be
        // present for the may-pay-{X} payment during the begin-combat trigger
        driver.giveMana(driver.player1, Color.BLUE, 5)

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(driver.player1, false)

        // Declined → nothing happens; no target selection.
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe false
        plusCounters(driver, tester) shouldBe 3
        plusCounters(driver, other) shouldBe 0
    }
})
