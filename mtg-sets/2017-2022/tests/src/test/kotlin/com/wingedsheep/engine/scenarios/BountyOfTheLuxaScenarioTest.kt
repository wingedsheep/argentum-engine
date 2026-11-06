package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.BountyOfTheLuxa
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bounty of the Luxa (AKH #196) — "At the beginning of your first main phase, remove all flood
 * counters from this enchantment. If no counters were removed this way, put a flood counter on this
 * enchantment and draw a card. Otherwise, add {C}{G}{U}."
 *
 * The branch hangs on whether the removal actually took counters off, not on a count read before
 * it, and the mana branch must leave {C}{G}{U} in the pool for the main phase it was made in.
 */
class BountyOfTheLuxaScenarioTest : FunSpec({

    /** Player1's turn, stopped in their upkeep — before the first-main trigger would fire. */
    fun driverAtUpkeep(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BountyOfTheLuxa)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.UPKEEP)
        return d
    }

    fun GameTestDriver.floodCounters(bounty: EntityId): Int =
        state.getEntity(bounty)?.get<CountersComponent>()?.getCount(CounterType.FLOOD) ?: 0

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("with no flood counter it puts one on and draws a card, adding no mana") {
        val d = driverAtUpkeep()
        val bounty = d.putPermanentOnBattlefield(d.player1, "Bounty of the Luxa")

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = d.getHandSize(d.player1)
        d.bothPass()

        d.floodCounters(bounty) shouldBe 1
        d.getHandSize(d.player1) shouldBe handBefore + 1
        val pool = d.pool(d.player1)
        (pool.colorless + pool.green + pool.blue) shouldBe 0
    }

    test("with a flood counter it removes it and adds {C}{G}{U} without drawing") {
        val d = driverAtUpkeep()
        val bounty = d.putPermanentOnBattlefield(d.player1, "Bounty of the Luxa")
        d.addComponent(bounty, CountersComponent(mapOf(CounterType.FLOOD to 1)))

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = d.getHandSize(d.player1)
        d.bothPass()

        d.floodCounters(bounty) shouldBe 0
        d.getHandSize(d.player1) shouldBe handBefore
        withClue("the mana is still in the pool during the first main phase") {
            d.state.step shouldBe Step.PRECOMBAT_MAIN
            val pool = d.pool(d.player1)
            pool.colorless shouldBe 1
            pool.green shouldBe 1
            pool.blue shouldBe 1
        }
    }

    test("several flood counters still yield only one {C}{G}{U}") {
        val d = driverAtUpkeep()
        val bounty = d.putPermanentOnBattlefield(d.player1, "Bounty of the Luxa")
        d.addComponent(bounty, CountersComponent(mapOf(CounterType.FLOOD to 3)))

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.bothPass()

        d.floodCounters(bounty) shouldBe 0
        val pool = d.pool(d.player1)
        pool.colorless shouldBe 1
        pool.green shouldBe 1
        pool.blue shouldBe 1
    }
})
