package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.RotisserieElemental
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rotisserie Elemental (WOE) — {R} Creature — Elemental, 1/1, menace.
 *
 * "Whenever this creature deals combat damage to a player, put a skewer counter on this creature.
 * Then you may sacrifice it. If you do, exile the top X cards of your library, where X is the
 * number of skewer counters on this creature. You may play those cards this turn."
 *
 * The two things worth pinning are the ones the implementation had to reason about: the skewer
 * tally accumulates across combats and survives a declined sacrifice, and X is the *post-increment*
 * count even though the same resolution sacrifices the creature that carried the counters.
 */
class RotisserieElementalScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RotisserieElemental))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Fully resolve the stack, resolving every triggered ability that lands on it. */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            bothPass()
            guard++
        }
    }

    fun GameTestDriver.skewerCount(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SKEWER) ?: 0

    fun GameTestDriver.exileSize(playerId: EntityId): Int =
        state.getZone(ZoneKey(playerId, Zone.EXILE)).size

    fun GameTestDriver.librarySize(playerId: EntityId): Int =
        state.getZone(ZoneKey(playerId, Zone.LIBRARY)).size

    test("connecting adds a skewer counter; declining the sacrifice leaves it on the battlefield") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val elemental = driver.putCreatureOnBattlefield(me, "Rotisserie Elemental")
        driver.removeSummoningSickness(elemental)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(elemental), opponent)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        driver.resolveStack()

        // The may-sacrifice prompt is the trigger resolving; decline it.
        driver.submitYesNo(me, false)
        driver.resolveStack()

        driver.skewerCount(elemental) shouldBe 1
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(elemental) shouldBe true
        driver.exileSize(me) shouldBe 0
    }

    test("accepting the sacrifice exiles that many cards and puts the Elemental in the graveyard") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val elemental = driver.putCreatureOnBattlefield(me, "Rotisserie Elemental")
        driver.removeSummoningSickness(elemental)

        val libraryBefore = driver.librarySize(me)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(elemental), opponent)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        driver.resolveStack()

        driver.submitYesNo(me, true)
        driver.resolveStack()

        // One skewer counter had just been added, so X is 1 — read off the creature the same
        // resolution sacrifices.
        driver.exileSize(me) shouldBe 1
        driver.librarySize(me) shouldBe libraryBefore - 1
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(elemental) shouldBe true
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(elemental) shouldBe false
    }

    test("skewer counters accumulate across combats, so X grows") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val elemental = driver.putCreatureOnBattlefield(me, "Rotisserie Elemental")
        driver.removeSummoningSickness(elemental)

        // First combat — decline.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(elemental), opponent)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        driver.resolveStack()
        driver.submitYesNo(me, false)
        driver.resolveStack()
        driver.skewerCount(elemental) shouldBe 1

        // Cycle back round to our next combat and connect again.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(elemental), opponent)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        driver.resolveStack()
        driver.skewerCount(elemental) shouldBe 2

        val libraryBefore = driver.librarySize(me)
        driver.submitYesNo(me, true)
        driver.resolveStack()

        driver.exileSize(me) shouldBe 2
        driver.librarySize(me) shouldBe libraryBefore - 2
    }
})
