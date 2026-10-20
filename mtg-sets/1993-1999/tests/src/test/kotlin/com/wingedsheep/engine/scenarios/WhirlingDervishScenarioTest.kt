package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.WhirlingDervish
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Whirling Dervish — {G}{G} 1/1 (Legends #211)
 *
 * "Protection from black. At the beginning of each end step, if this creature dealt damage to an
 * opponent this turn, put a +1/+1 counter on it."
 *
 * The intervening-if is `CountPlayersWith(EachOpponent, SourceDealtDamageToPlayerThisTurn(You))`,
 * which relies on the per-player rebind of `Player.You` while still reading the Dervish's own
 * damage memory — so the cases that matter are: the Dervish's own damage to the opponent counts,
 * damage dealt only to a blocker doesn't, another creature's damage to the opponent doesn't, and
 * the memory is per turn (the next end step adds nothing).
 */
class WhirlingDervishScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + WhirlingDervish)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Advance into the end step and resolve whatever triggered there. */
    fun GameTestDriver.resolveEndStep() {
        passPriorityUntil(Step.END)
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("dealing combat damage to the opponent puts a counter on it at the end step, once per turn") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val dervish = d.putCreatureOnBattlefield(d.player1, "Whirling Dervish")
        d.removeSummoningSickness(dervish)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(dervish), opponent).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(opponent)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 19

        d.resolveEndStep()
        withClue("it dealt damage to an opponent this turn") { d.plusOneCounters(dervish) shouldBe 1 }

        // The opponent's turn: the Dervish dealt no damage this turn, so their end step adds nothing.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        d.resolveEndStep()
        withClue("the damage memory is per turn") { d.plusOneCounters(dervish) shouldBe 1 }
    }

    test("damage dealt only to a blocker does not count") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val dervish = d.putCreatureOnBattlefield(d.player1, "Whirling Dervish")
        d.removeSummoningSickness(dervish)
        val wall = d.putCreatureOnBattlefield(opponent, "Wall of Wood")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(dervish), opponent).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opponent, mapOf(wall to listOf(dervish))).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 20

        d.resolveEndStep()
        d.plusOneCounters(dervish) shouldBe 0
    }

    test("another creature's damage to the opponent does not count") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val dervish = d.putCreatureOnBattlefield(d.player1, "Whirling Dervish")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bears)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(bears), opponent).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(opponent)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 18

        d.resolveEndStep()
        d.plusOneCounters(dervish) shouldBe 0
    }
})
