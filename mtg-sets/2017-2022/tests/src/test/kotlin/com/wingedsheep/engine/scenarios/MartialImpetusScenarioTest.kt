package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c20.cards.MartialImpetus
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Martial Impetus (C20) — {2}{W} Enchantment — Aura.
 *
 * "Enchant creature. Enchanted creature gets +1/+1 and is goaded. Whenever enchanted creature
 * attacks, each other creature that's attacking one of your opponents gets +1/+1 until end of turn."
 *
 * Three-player game (two-player where noted): Player 1 enchants Player 2's Grizzly Bears, then Player 2's turn comes up.
 */
class MartialImpetusScenarioTest : FunSpec({

    data class Table(val d: GameTestDriver, val p1: EntityId, val p2: EntityId, val p3: EntityId?, val bears: EntityId, val giant: EntityId, val aura: EntityId)

    fun setup(players: Int = 3): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MartialImpetus)
        val seats = d.initMultiplayer(List(players) { Deck.of("Plains" to 40) })
        val (p1, p2) = seats
        val p3 = seats.getOrNull(2)
        val bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(p2, "Hill Giant")
        d.removeSummoningSickness(bears)
        d.removeSummoningSickness(giant)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1
        val aura = d.putCardInHand(p1, "Martial Impetus")
        d.giveMana(p1, Color.WHITE, 3)
        d.castSpell(p1, aura, listOf(bears)).outcome shouldBe Outcome.Done
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 10) d.passPriority(d.state.priorityPlayerId!!)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS, p2)
        return Table(d, p1, p2, p3, bears, giant, aura)
    }

    test("the enchanted creature gets +1/+1 and must attack") {
        val t = setup()
        t.d.state.projectedState.getPower(t.bears) shouldBe 3
        t.d.state.projectedState.getToughness(t.bears) shouldBe 3
        t.d.declareAttackers(t.p2, emptyMap()).outcome shouldNotBe Outcome.Done
    }

    test("it must attack a player other than the Aura's controller while one is available") {
        val t = setup()
        withClue("attacking the goader is refused while Player 3 can be attacked") {
            t.d.declareAttackers(t.p2, mapOf(t.bears to t.p1)).outcome shouldNotBe Outcome.Done
        }
        t.d.declareAttackers(t.p2, mapOf(t.bears to t.p3!!)).outcome shouldBe Outcome.Done
    }

    test("its attack pumps each other creature attacking one of the Aura controller's opponents") {
        val t = setup()
        t.d.declareAttackers(t.p2, mapOf(t.bears to t.p3!!, t.giant to t.p3!!)).outcome shouldBe Outcome.Done
        var guard = 0
        while (t.d.state.stack.isNotEmpty() && guard++ < 10) t.d.passPriority(t.d.state.priorityPlayerId!!)

        withClue("Hill Giant attacks Player 3, an opponent of Player 1") {
            t.d.state.projectedState.getPower(t.giant) shouldBe 4
            t.d.state.projectedState.getToughness(t.giant) shouldBe 4
        }
        withClue("the enchanted creature itself isn't 'other'") {
            t.d.state.projectedState.getPower(t.bears) shouldBe 3
        }
    }

    test("a creature attacking the Aura's controller gets no bonus") {
        val t = setup()
        t.d.declareAttackers(t.p2, mapOf(t.bears to t.p3!!, t.giant to t.p1)).outcome shouldBe Outcome.Done
        var guard = 0
        while (t.d.state.stack.isNotEmpty() && guard++ < 10) t.d.passPriority(t.d.state.priorityPlayerId!!)

        t.d.state.projectedState.getPower(t.giant) shouldBe 3
    }

    test("the goad ends when the Aura leaves — there is no until-your-next-turn designation left behind") {
        val t = setup()
        t.d.moveToGraveyard(t.aura)
        t.d.declareAttackers(t.p2, emptyMap()).outcome shouldBe Outcome.Done
    }

    test("with the goader as the only opponent, attacking the goader satisfies the goad") {
        val t = setup(players = 2)
        t.d.declareAttackers(t.p2, emptyMap()).outcome shouldNotBe Outcome.Done
        t.d.declareAttackers(t.p2, mapOf(t.bears to t.p1)).outcome shouldBe Outcome.Done
    }
})
