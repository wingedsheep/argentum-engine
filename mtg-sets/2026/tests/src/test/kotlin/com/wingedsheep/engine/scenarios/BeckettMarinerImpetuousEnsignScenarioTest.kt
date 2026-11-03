package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.BeckettMarinerImpetuousEnsign
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Beckett Mariner, Impetuous Ensign {1}{R} — Legendary Creature — Human Officer 2/1
 *   First strike
 *   Reckless Behavior — Whenever Beckett Mariner becomes tapped, put a promotion counter on her. Then if
 *   there are three or more promotion counters on her, remove those counters and she deals 2 damage to
 *   each opponent.
 *
 * Pins that tapping (here, attacking) adds a promotion counter, that below three nothing else happens,
 * and that the third counter clears them all and deals 2 to the opponent.
 */
class BeckettMarinerImpetuousEnsignScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val me: EntityId, val opp: EntityId, val beckett: EntityId)

    fun table(startingCounters: Int): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BeckettMarinerImpetuousEnsign)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val beckett = d.putCreatureOnBattlefield(me, "Beckett Mariner, Impetuous Ensign")
        d.removeSummoningSickness(beckett)
        if (startingCounters > 0) {
            d.replaceState(d.state.updateEntity(beckett) {
                it.with(CountersComponent(mapOf(CounterType.PROMOTION to startingCounters)))
            })
        }
        return Table(d, me, opp, beckett)
    }

    fun Table.promotions(): Int =
        d.state.getEntity(beckett)?.get<CountersComponent>()?.getCount(CounterType.PROMOTION) ?: 0

    fun Table.attackAndResolveTrigger() {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, mapOf(beckett to opp)).error shouldBe null
        d.stackSize shouldBe 1
        d.bothPass()
    }

    test("attacking taps her and adds a promotion counter, with no damage below three") {
        val t = table(startingCounters = 0)
        val life = t.d.getLifeTotal(t.opp)
        t.attackAndResolveTrigger()

        withClue("one promotion counter") { t.promotions() shouldBe 1 }
        withClue("no damage yet") { t.d.getLifeTotal(t.opp) shouldBe life }
    }

    test("the third counter is removed with the others and she deals 2 damage to each opponent") {
        val t = table(startingCounters = 2)
        val life = t.d.getLifeTotal(t.opp)
        t.attackAndResolveTrigger()

        withClue("all promotion counters removed") { t.promotions() shouldBe 0 }
        withClue("2 damage to the opponent") { t.d.getLifeTotal(t.opp) shouldBe life - 2 }
    }
})
