package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.answerForcedPayments
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.cumulativeUpkeep
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cumulative upkeep (CR 702.24), through the `cumulativeUpkeep(cost)` builder.
 *
 *  - 702.24a: at the beginning of your upkeep, *if this permanent is on the battlefield*, put an age
 *    counter on it; then you may pay [cost] for each age counter on it; if you don't, sacrifice it.
 *    The whole set of costs is paid or none of it — no partial payment.
 *  - 702.24b: multiple instances trigger separately, and each counts every age counter on the
 *    permanent as it resolves.
 */
class CumulativeUpkeepTest : FunSpec({

    val oneGeneric = card("Test Cumulative One") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "Cumulative upkeep {1}"
        cumulativeUpkeep(ManaCost.parse("{1}"))
    }
    val green = card("Test Cumulative Green") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        oracleText = "Cumulative upkeep {G}"
        cumulativeUpkeep(ManaCost.parse("{G}"))
    }
    val twice = card("Test Cumulative Twice") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "Cumulative upkeep {1}\nCumulative upkeep {1}"
        cumulativeUpkeep(ManaCost.parse("{1}"))
        cumulativeUpkeep(ManaCost.parse("{1}"))
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(oneGeneric, green, twice))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.ageCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.AGE) ?: 0

    fun GameTestDriver.onBattlefield(id: EntityId): Boolean = id in state.getBattlefield()

    /** Advance to [me]'s next upkeep, with the cumulative upkeep trigger(s) on the stack. */
    fun GameTestDriver.toMyNextUpkeep(me: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN, getOpponent(me))
        passPriorityUntil(Step.UPKEEP, me)
    }

    test("an age counter, then pay {1} per counter: it stays; a turn later the bill is {2}") {
        val d = driver()
        val me = d.activePlayer!!
        val permanent = d.putPermanentOnBattlefield(me, "Test Cumulative One")

        d.toMyNextUpkeep(me)
        d.giveColorlessMana(me, 1)
        d.bothPass()
        d.ageCounters(permanent) shouldBe 1
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe me
        d.submitYesNo(me, true)
        d.onBattlefield(permanent) shouldBe true

        d.toMyNextUpkeep(me)
        d.giveColorlessMana(me, 1)
        d.bothPass()
        // Two age counters cost {2}: one floating mana can't pay, so refusing is the only answer.
        d.answerForcedPayments()
        d.ageCounters(permanent) shouldBe 0
        d.onBattlefield(permanent) shouldBe false
        d.getGraveyard(me) shouldContain permanent
    }

    test("the second upkeep charges for both age counters") {
        val d = driver()
        val me = d.activePlayer!!
        val permanent = d.putPermanentOnBattlefield(me, "Test Cumulative One")
        d.replaceState(d.state.updateEntity(permanent) { it.with(CountersComponent().withAdded(CounterType.AGE, 1)) })

        d.toMyNextUpkeep(me)
        d.giveColorlessMana(me, 2)
        d.bothPass()
        d.ageCounters(permanent) shouldBe 2
        d.submitYesNo(me, true)
        d.onBattlefield(permanent) shouldBe true
        d.state.getEntity(me)!!.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()!!.colorless shouldBe 0
    }

    test("declining to pay sacrifices it") {
        val d = driver()
        val me = d.activePlayer!!
        val permanent = d.putPermanentOnBattlefield(me, "Test Cumulative One")

        d.toMyNextUpkeep(me)
        d.giveColorlessMana(me, 5)
        d.bothPass()
        d.submitYesNo(me, false)
        d.onBattlefield(permanent) shouldBe false
        d.getGraveyard(me) shouldContain permanent
    }

    test("a colored cost multiplies its colored symbol: two age counters need {G}{G}") {
        val d = driver()
        val me = d.activePlayer!!
        val permanent = d.putPermanentOnBattlefield(me, "Test Cumulative Green")
        d.replaceState(d.state.updateEntity(permanent) { it.with(CountersComponent().withAdded(CounterType.AGE, 1)) })

        d.toMyNextUpkeep(me)
        // {G} plus a generic mana is two mana but not {G}{G}: unaffordable, so it's sacrificed.
        d.giveMana(me, Color.GREEN, 1)
        d.giveColorlessMana(me, 1)
        d.bothPass()
        d.answerForcedPayments()
        d.onBattlefield(permanent) shouldBe false
    }

    test("two instances trigger separately and each counts every age counter (CR 702.24b)") {
        val d = driver()
        val me = d.activePlayer!!
        val permanent = d.putPermanentOnBattlefield(me, "Test Cumulative Twice")

        d.toMyNextUpkeep(me)
        d.giveColorlessMana(me, 3)
        var paid = 0
        var guard = 0
        while ((d.stackSize > 0 || d.pendingDecision != null) && guard++ < 10) {
            val decision = d.pendingDecision
            if (decision is YesNoDecision) {
                d.submitYesNo(me, true); paid++
            } else if (decision != null) {
                d.autoResolveDecision()
            } else {
                d.bothPass()
            }
        }
        paid shouldBe 2
        d.ageCounters(permanent) shouldBe 2
        // The first resolution paid {1} for one counter, the second {2} for both.
        d.state.getEntity(me)!!.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()!!.colorless shouldBe 0
        d.onBattlefield(permanent) shouldBe true
    }

    test("if it left the battlefield before the trigger resolves, nothing happens (intervening if)") {
        val d = driver()
        val me = d.activePlayer!!
        val permanent = d.putPermanentOnBattlefield(me, "Test Cumulative One")

        d.toMyNextUpkeep(me)
        d.stackSize shouldBe 1
        d.moveToGraveyard(permanent)
        d.bothPass()
        d.pendingDecision shouldBe null
        d.ageCounters(permanent) shouldBe 0
    }

    test("it doesn't trigger on an opponent's upkeep") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Test Cumulative One")

        d.passPriorityUntil(Step.UPKEEP, opp)
        d.stackSize shouldBe 0
    }

    test("costs a single dynamic payment can't express are rejected at authoring time") {
        shouldThrow<IllegalArgumentException> {
            card("Test Cumulative Mixed") {
                manaCost = "{U}"
                typeLine = "Enchantment"
                cumulativeUpkeep(ManaCost.parse("{1}{U}"))
            }
        }
    }
})
