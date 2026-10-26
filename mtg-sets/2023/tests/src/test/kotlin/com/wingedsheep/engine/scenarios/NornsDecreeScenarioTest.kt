package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onc.cards.NornsDecree
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Norn's Decree (ONC #9) — {2}{W} Enchantment.
 *
 *   Whenever one or more creatures an opponent controls deal combat damage to you, that opponent
 *   gets a poison counter.
 *   Whenever a player attacks, if one or more players being attacked are poisoned, the attacking
 *   player draws a card.
 */
class NornsDecreeScenarioTest : FunSpec({

    val walker = card("Test Decree Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun threePlayer(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NornsDecree, walker))
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe players[0]
        return d to players
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.poison(player: EntityId, count: Int) {
        replaceState(state.updateEntity(player) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) })
    }

    fun GameTestDriver.poisonOf(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("an opponent's creatures dealing you combat damage give that opponent one poison counter") {
        val (d, p) = threePlayer()
        d.putPermanentOnBattlefield(p[1], "Norn's Decree")
        val a = d.ready(p[0], "Grizzly Bears")
        val b = d.ready(p[0], "Grizzly Bears")
        val hand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(a to p[1], b to p[1])).error shouldBe null
        withClue("nobody being attacked is poisoned yet, so the attack trigger doesn't fire") { d.stackSize shouldBe 0 }
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.getLifeTotal(p[1]) shouldBe 16
        withClue("one counter for the batch, however many creatures connected") { d.poisonOf(p[0]) shouldBe 1 }
        d.poisonOf(p[1]) shouldBe 0
        d.poisonOf(p[2]) shouldBe 0
        d.getHandSize(p[0]) shouldBe hand
    }

    test("a player attacking a poisoned player draws a card, whoever controls the Decree") {
        val (d, p) = threePlayer()
        d.putPermanentOnBattlefield(p[2], "Norn's Decree")
        d.poison(p[1], 1)
        val bears = d.ready(p[0], "Grizzly Bears")
        val attackerHand = d.getHandSize(p[0])
        val ownerHand = d.getHandSize(p[2])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(bears to p[1])).error shouldBe null
        d.stackSize shouldBe 1
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        d.getHandSize(p[0]) shouldBe attackerHand + 1
        d.getHandSize(p[2]) shouldBe ownerHand
    }

    test("attacking only an unpoisoned player draws nothing, even if another opponent is poisoned") {
        val (d, p) = threePlayer()
        d.putPermanentOnBattlefield(p[1], "Norn's Decree")
        d.poison(p[1], 4)
        val bears = d.ready(p[0], "Grizzly Bears")
        val hand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(bears to p[2])).error shouldBe null

        d.stackSize shouldBe 0
        d.getHandSize(p[0]) shouldBe hand
    }

    test("attacking only a poisoned player's planeswalker doesn't count that player as being attacked") {
        val (d, p) = threePlayer()
        d.putPermanentOnBattlefield(p[1], "Norn's Decree")
        d.poison(p[1], 3)
        val pw = d.putPermanentOnBattlefield(p[1], "Test Decree Walker")
        d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        val bears = d.ready(p[0], "Grizzly Bears")
        val hand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(bears to pw)).error shouldBe null

        d.stackSize shouldBe 0
        d.getHandSize(p[0]) shouldBe hand
    }
})
