package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.CrystallineGiant
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Crystalline Giant (IKO #234) — {3} Artifact Creature — Giant, 3/3.
 *
 * "At the beginning of combat on your turn, choose a kind of counter at random that this creature
 * doesn't have on it from among flying, first strike, deathtouch, hexproof, lifelink, menace,
 * reach, trample, vigilance, and +1/+1. Put a counter of that kind on this creature."
 *
 * Pins: one counter of a listed kind with no prompt, never a kind it already has, the last missing
 * kind when only one is left, nothing once it has all ten, and no trigger on the opponent's turn.
 */
class CrystallineGiantScenarioTest : FunSpec({

    val listed = listOf(
        CounterType.FLYING, CounterType.FIRST_STRIKE, CounterType.DEATHTOUCH, CounterType.HEXPROOF,
        CounterType.LIFELINK, CounterType.MENACE, CounterType.REACH, CounterType.TRAMPLE,
        CounterType.VIGILANCE, CounterType.PLUS_ONE_PLUS_ONE
    )

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(CrystallineGiant))
        initMirrorMatch(Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.setCounters(id: EntityId, counters: Map<CounterType, Int>) {
        replaceState(state.updateEntity(id) { it.with(CountersComponent(counters)) })
    }

    fun GameTestDriver.counters(id: EntityId): Map<CounterType, Int> =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.filterValues { it > 0 }.orEmpty()

    test("at beginning of combat it gets one counter of a listed kind, chosen without a prompt") {
        val d = setup()
        val giant = d.putCreatureOnBattlefield(d.activePlayer!!, "Crystalline Giant")

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.stackSize shouldBe 1
        d.bothPass()

        d.pendingDecision shouldBe null
        val got = d.counters(giant)
        got.size shouldBe 1
        got.values.single() shouldBe 1
        (got.keys.single() in listed) shouldBe true
    }

    test("it never gets a kind it already has: with one kind missing, that one is placed") {
        val d = setup()
        val giant = d.putCreatureOnBattlefield(d.activePlayer!!, "Crystalline Giant")
        d.setCounters(giant, (listed - CounterType.MENACE).associateWith { 1 })

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.bothPass()

        d.counters(giant) shouldBe listed.associateWith { 1 }
        d.state.projectedState.hasKeyword(giant, Keyword.MENACE) shouldBe true
        d.state.projectedState.getPower(giant) shouldBe 4
    }

    test("with all ten kinds already on it, nothing is added") {
        val d = setup()
        val giant = d.putCreatureOnBattlefield(d.activePlayer!!, "Crystalline Giant")
        d.setCounters(giant, listed.associateWith { 1 })

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.bothPass()

        d.counters(giant) shouldBe listed.associateWith { 1 }
    }

    test("it doesn't trigger at the beginning of combat on an opponent's turn") {
        val d = setup()
        val opponent = d.getOpponent(d.activePlayer!!)
        val giant = d.putCreatureOnBattlefield(opponent, "Crystalline Giant")

        d.passPriorityUntil(Step.BEGIN_COMBAT)

        d.stackSize shouldBe 0
        d.counters(giant) shouldBe emptyMap()
    }
})
