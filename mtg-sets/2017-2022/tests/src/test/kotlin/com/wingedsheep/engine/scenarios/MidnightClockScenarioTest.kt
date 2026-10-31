package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.MidnightClock
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Midnight Clock — "{2}{U}: Put an hour counter on this artifact. At the beginning of each upkeep,
 * put an hour counter on this artifact. When the twelfth hour counter is put on this artifact,
 * shuffle your hand and graveyard into your library, then draw seven cards. Exile this artifact."
 */
class MidnightClockScenarioTest : FunSpec({
    val hourAbility = MidnightClock.activatedAbilities.first { !it.isManaAbility }.id

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(MidnightClock)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.hours(clock: EntityId): Int =
        state.getEntity(clock)?.get<CountersComponent>()?.getCount(CounterType.HOUR) ?: 0

    fun GameTestDriver.setHours(clock: EntityId, n: Int) =
        replaceState(state.updateEntity(clock) { it.with(CountersComponent().withAdded(CounterType.HOUR, n)) })

    fun GameTestDriver.tickClock(you: EntityId, clock: EntityId) {
        giveMana(you, Color.BLUE, 1)
        giveColorlessMana(you, 2)
        submitSuccess(ActivateAbility(you, clock, hourAbility))
        bothPass()
    }

    test("the twelfth hour counter resets hand and graveyard into seven fresh cards and exiles the Clock") {
        val d = driver(); val you = d.activePlayer!!
        val clock = d.putPermanentOnBattlefield(you, "Midnight Clock")
        d.setHours(clock, 10)
        repeat(2) { d.putCardInGraveyard(you, "Island") }

        d.tickClock(you, clock)
        withClue("the eleventh counter doesn't trigger the reset") {
            d.hours(clock) shouldBe 11
            d.getGraveyard(you).size shouldBe 2
        }

        val library = d.state.getLibrary(you).size
        val hand = d.getHandSize(you)
        d.tickClock(you, clock)
        d.bothPass() // resolve the twelfth-hour trigger

        d.getHandSize(you) shouldBe 7
        d.getGraveyard(you).size shouldBe 0
        d.state.getLibrary(you).size shouldBe library + hand + 2 - 7
        d.getExile(you) shouldContain clock
        d.state.getBattlefield() shouldNotContain clock
    }

    test("a thirteenth counter does not trigger it again") {
        val d = driver(); val you = d.activePlayer!!
        val clock = d.putPermanentOnBattlefield(you, "Midnight Clock")
        d.setHours(clock, 12)
        val hand = d.getHandSize(you)

        d.tickClock(you, clock)

        d.hours(clock) shouldBe 13
        d.state.getBattlefield() shouldContain clock
        d.getHandSize(you) shouldBe hand
    }

    test("it gains an hour counter at the beginning of each upkeep, the opponent's included") {
        val d = driver(); val you = d.activePlayer!!; val opp = d.getOpponent(you)
        val clock = d.putPermanentOnBattlefield(you, "Midnight Clock")

        d.passPriorityUntil(Step.DRAW, opp)
        d.hours(clock) shouldBe 1
        d.passPriorityUntil(Step.DRAW, you)
        d.hours(clock) shouldBe 2
    }
})
