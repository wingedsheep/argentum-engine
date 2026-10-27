package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c21.cards.BreenaTheDemagogue
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Breena, the Demagogue (C21 #1): whenever a player attacks one of your opponents, if that opponent
 * has more life than another of your opponents, that attacking player draws a card and you put two
 * +1/+1 counters on a creature you control.
 */
class BreenaTheDemagogueScenarioTest : FunSpec({

    fun driver(): Triple<GameTestDriver, EntityId, Pair<EntityId, EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(BreenaTheDemagogue))
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1
        return Triple(d, p1, p2 to p3)
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 20) bothPass()
    }

    test("an opponent attacks your richer opponent: the attacker draws, Breena's controller gets counters") {
        val (d, p1, others) = driver()
        val (p2, p3) = others
        val breena = d.putCreatureOnBattlefield(p2, "Breena, the Demagogue")
        val bears = d.ready(p1, "Grizzly Bears")
        // The attacking player is one of Breena's opponents with less life than the attacked one (ruling).
        d.setLifeTotal(p1, 15)
        val p1Hand = d.getHandSize(p1)
        val p2Hand = d.getHandSize(p2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p3)).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()
        (d.pendingDecision as? ChooseTargetsDecision)?.let { d.submitTargetSelection(p2, listOf(breena)) }
        d.resolveAll()

        withClue("that attacking player draws") { d.getHandSize(p1) shouldBe p1Hand + 1 }
        d.getHandSize(p2) shouldBe p2Hand
        withClue("Breena's controller puts the counters on a creature they control") { d.plusCounters(breena) shouldBe 2 }
    }

    test("no trigger when the attacked opponent doesn't have more life than another opponent") {
        val (d, p1, others) = driver()
        val (p2, p3) = others
        val breena = d.putCreatureOnBattlefield(p2, "Breena, the Demagogue")
        val bears = d.ready(p1, "Grizzly Bears")
        val p1Hand = d.getHandSize(p1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p3)).error shouldBe null
        withClue("everyone is at 20 — a tie isn't more life") { d.stackSize shouldBe 0 }
        d.getHandSize(p1) shouldBe p1Hand
        d.plusCounters(breena) shouldBe 0
    }

    test("an attack on Breena's controller isn't an attack on one of their opponents") {
        val (d, p1, others) = driver()
        val (p2, _) = others
        d.putCreatureOnBattlefield(p2, "Breena, the Demagogue")
        val bears = d.ready(p1, "Grizzly Bears")
        d.setLifeTotal(p1, 5)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p2)).error shouldBe null
        d.stackSize shouldBe 0
    }

    test("your own attack on two opponents fires only for the one with more life; you choose the creature") {
        val (d, p1, others) = driver()
        val (p2, p3) = others
        val breena = d.putCreatureOnBattlefield(p1, "Breena, the Demagogue")
        val a = d.ready(p1, "Grizzly Bears")
        val b = d.ready(p1, "Grizzly Bears")
        d.setLifeTotal(p3, 10)
        val hand = d.getHandSize(p1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(a to p2, b to p3)).error shouldBe null
        withClue("p2 (20) has more life than p3 (10); p3 has no poorer opponent beside it") { d.stackSize shouldBe 1 }
        d.resolveAll()
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe p1
        d.submitTargetSelection(p1, listOf(b)).error shouldBe null
        d.resolveAll()

        d.getHandSize(p1) shouldBe hand + 1
        d.plusCounters(b) shouldBe 2
        d.plusCounters(a) shouldBe 0
        d.plusCounters(breena) shouldBe 0
    }
})
