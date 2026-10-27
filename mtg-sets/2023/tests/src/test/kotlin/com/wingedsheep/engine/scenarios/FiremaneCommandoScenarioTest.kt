package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.moc.cards.FiremaneCommando
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Firemane Commando (MOC #73) — {3}{W} 4/3 Angel Soldier, flying.
 *
 *   Whenever you attack with two or more creatures, draw a card.
 *   Whenever another player attacks with two or more creatures, they draw a card if none of those
 *   creatures attacked you.
 */
class FiremaneCommandoScenarioTest : FunSpec({

    val walker = card("Test Commando Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    /** Three players; p[0] is active and attacks, p[1] controls the Commando unless a test says otherwise. */
    fun threePlayer(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(FiremaneCommando, walker))
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe players[0]
        return d to players
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    test("you attack with two or more creatures: you draw a card") {
        val (d, p) = threePlayer()
        d.putCreatureOnBattlefield(p[0], "Firemane Commando")
        val a = d.ready(p[0], "Grizzly Bears")
        val b = d.ready(p[0], "Grizzly Bears")
        val hand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(a to p[1], b to p[2])).error shouldBe null
        withClue("only the 'you attack' ability — your own attack isn't another player's") { d.stackSize shouldBe 1 }
        d.resolveStack()

        d.getHandSize(p[0]) shouldBe hand + 1
    }

    test("attacking with a single creature triggers neither ability") {
        val (d, p) = threePlayer()
        d.putCreatureOnBattlefield(p[0], "Firemane Commando")
        d.putCreatureOnBattlefield(p[1], "Firemane Commando")
        val a = d.ready(p[0], "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(a to p[2])).error shouldBe null

        d.stackSize shouldBe 0
    }

    test("another player attacks someone else with two creatures: they draw, you don't") {
        val (d, p) = threePlayer()
        d.putCreatureOnBattlefield(p[1], "Firemane Commando")
        val a = d.ready(p[0], "Grizzly Bears")
        val b = d.ready(p[0], "Grizzly Bears")
        val attackerHand = d.getHandSize(p[0])
        val ownerHand = d.getHandSize(p[1])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(a to p[2], b to p[2])).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveStack()

        withClue("'they' — the attacking player — draws") { d.getHandSize(p[0]) shouldBe attackerHand + 1 }
        d.getHandSize(p[1]) shouldBe ownerHand
    }

    test("one of those creatures attacked you: the ability triggers but nobody draws") {
        val (d, p) = threePlayer()
        d.putCreatureOnBattlefield(p[1], "Firemane Commando")
        val a = d.ready(p[0], "Grizzly Bears")
        val b = d.ready(p[0], "Grizzly Bears")
        val attackerHand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(a to p[1], b to p[2])).error shouldBe null
        withClue("the draw is a resolution check, not a trigger condition") { d.stackSize shouldBe 1 }
        d.resolveStack()

        d.getHandSize(p[0]) shouldBe attackerHand
    }

    test("removing the creature that attacked you in response doesn't let them draw") {
        val (d, p) = threePlayer()
        d.putCreatureOnBattlefield(p[1], "Firemane Commando")
        val a = d.ready(p[0], "Grizzly Bears")
        val b = d.ready(p[0], "Grizzly Bears")
        val attackerHand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(a to p[1], b to p[2])).error shouldBe null
        d.stackSize shouldBe 1
        d.replaceState(d.state.updateEntity(a) { it.without<AttackingComponent>() })
        d.resolveStack()

        withClue("ruling: once it triggers, what happens to the attackers doesn't matter") {
            d.getHandSize(p[0]) shouldBe attackerHand
        }
    }

    test("attacking only your planeswalker isn't attacking you: they draw") {
        val (d, p) = threePlayer()
        d.putCreatureOnBattlefield(p[1], "Firemane Commando")
        val pw = d.putPermanentOnBattlefield(p[1], "Test Commando Walker")
        d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        val a = d.ready(p[0], "Grizzly Bears")
        val b = d.ready(p[0], "Grizzly Bears")
        val attackerHand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(a to pw, b to pw)).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveStack()

        d.getHandSize(p[0]) shouldBe attackerHand + 1
    }
})
