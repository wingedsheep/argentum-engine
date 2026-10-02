package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.one.cards.JorKadeenFirstGoldwarden
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Jor Kadeen, First Goldwarden (ONE #203) — {R}{W} 2/2 trample.
 *
 * "Whenever Jor Kadeen attacks, it gets +X/+X until end of turn, where X is the number of
 *  equipped creatures you control. Then if Jor Kadeen's power is 4 or greater, draw a card."
 *
 * Covers: X counts every equipped creature you control (not just Jor Kadeen), the power check
 * reads Jor Kadeen's power *after* the pump, and the 4-power threshold is exact.
 */
class JorKadeenFirstGoldwardenScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + JorKadeenFirstGoldwarden + Bonesplitter)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.equip(equipment: EntityId, creature: EntityId) {
        addComponent(equipment, AttachedToComponent(creature))
        addComponent(creature, AttachmentsComponent(listOf(equipment)))
    }

    /** Attack with Jor Kadeen and resolve its trigger; returns hand-size delta. */
    fun GameTestDriver.attackAndResolve(jor: EntityId): Int {
        val p1 = activePlayer!!
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = getHandSize(p1)
        declareAttackers(p1, listOf(jor), getOpponent(p1)).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
        return getHandSize(p1) - handBefore
    }

    test("no equipped creatures: X = 0, power stays 2, no card drawn") {
        val d = setup()
        val p1 = d.activePlayer!!
        val jor = d.putCreatureOnBattlefield(p1, "Jor Kadeen, First Goldwarden")
        d.removeSummoningSickness(jor)

        d.attackAndResolve(jor) shouldBe 0
        d.state.projectedState.getPower(jor) shouldBe 2
        d.state.projectedState.getToughness(jor) shouldBe 2
    }

    test("one other equipped creature: X = 1, Jor is 3/3, below threshold, no draw") {
        val d = setup()
        val p1 = d.activePlayer!!
        val jor = d.putCreatureOnBattlefield(p1, "Jor Kadeen, First Goldwarden")
        d.removeSummoningSickness(jor)
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.equip(d.putPermanentOnBattlefield(p1, "Bonesplitter"), bear)

        d.attackAndResolve(jor) shouldBe 0
        d.state.projectedState.getPower(jor) shouldBe 3
        d.state.projectedState.getToughness(jor) shouldBe 3
    }

    test("two other equipped creatures: X = 2, Jor is 4/4, draws a card") {
        val d = setup()
        val p1 = d.activePlayer!!
        val jor = d.putCreatureOnBattlefield(p1, "Jor Kadeen, First Goldwarden")
        d.removeSummoningSickness(jor)
        val bear1 = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val bear2 = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.equip(d.putPermanentOnBattlefield(p1, "Bonesplitter"), bear1)
        d.equip(d.putPermanentOnBattlefield(p1, "Bonesplitter"), bear2)

        d.attackAndResolve(jor) shouldBe 1
        d.state.projectedState.getPower(jor) shouldBe 4
        d.state.projectedState.getToughness(jor) shouldBe 4
    }

    test("opponent's equipped creatures don't count") {
        val d = setup()
        val p1 = d.activePlayer!!
        val opp = d.getOpponent(p1)
        val jor = d.putCreatureOnBattlefield(p1, "Jor Kadeen, First Goldwarden")
        d.removeSummoningSickness(jor)
        val theirBear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.equip(d.putPermanentOnBattlefield(opp, "Bonesplitter"), theirBear)

        d.attackAndResolve(jor) shouldBe 0
        d.state.projectedState.getPower(jor) shouldBe 2
    }

    test("Jor Kadeen equipped with Bonesplitter: 4/2 +1/+1 = 5/3, draws a card") {
        val d = setup()
        val p1 = d.activePlayer!!
        val jor = d.putCreatureOnBattlefield(p1, "Jor Kadeen, First Goldwarden")
        d.removeSummoningSickness(jor)
        d.equip(d.putPermanentOnBattlefield(p1, "Bonesplitter"), jor)

        d.attackAndResolve(jor) shouldBe 1
        d.state.projectedState.getPower(jor) shouldBe 5
        d.state.projectedState.getToughness(jor) shouldBe 3
    }
})
