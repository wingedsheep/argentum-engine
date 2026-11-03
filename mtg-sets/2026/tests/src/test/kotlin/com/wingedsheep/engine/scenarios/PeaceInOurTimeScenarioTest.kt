package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.PeaceInOurTime
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Peace in Our Time {3}{W} — Enchantment
 *   At the beginning of combat on each player's turn, that player may draw a card. If they do, they
 *   can't attack this turn and you put a peace counter on this enchantment.
 *   At the beginning of your upkeep, if this enchantment has twelve or more peace counters on it, you
 *   win the game.
 *
 * Pins that the active player (not the enchantment's controller) makes the choice and draws, that the
 * attack lock covers creatures they get after the trigger, that the counter only lands on a draw, and
 * the twelve-counter upkeep win.
 */
class PeaceInOurTimeScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val me: EntityId, val opp: EntityId, val peace: EntityId)

    fun table(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PeaceInOurTime)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val peace = d.putPermanentOnBattlefield(me, "Peace in Our Time")
        return Table(d, me, opp, peace)
    }

    fun Table.peaceCounters(): Int =
        d.state.getEntity(peace)?.get<CountersComponent>()?.getCount(CounterType.PEACE) ?: 0

    fun Table.bears(owner: EntityId): EntityId =
        d.putCreatureOnBattlefield(owner, "Grizzly Bears").also { d.removeSummoningSickness(it) }

    /** Advance to [player]'s beginning of combat and resolve the trigger up to its may-draw prompt. */
    fun Table.toCombatPrompt(player: EntityId) {
        d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = player)
        d.stackSize shouldBe 1
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe player
    }

    test("the opponent draws on their turn - they can't attack, even with a creature gained afterwards") {
        val t = table()
        val bears = t.bears(t.opp)
        t.toCombatPrompt(t.opp)
        val handBefore = t.d.getHandSize(t.opp)
        t.d.submitYesNo(t.opp, true).error shouldBe null

        withClue("the active player draws") { t.d.getHandSize(t.opp) shouldBe handBefore + 1 }
        withClue("the enchantment's controller gets the counter") { t.peaceCounters() shouldBe 1 }

        val lateBears = t.bears(t.opp)
        t.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        t.d.declareAttackers(t.opp, mapOf(bears to t.me)).error shouldNotBe null
        t.d.declareAttackers(t.opp, mapOf(lateBears to t.me)).error shouldNotBe null
    }

    test("declining the draw leaves the attack open and adds no counter") {
        val t = table()
        val bears = t.bears(t.opp)
        t.toCombatPrompt(t.opp)
        val handBefore = t.d.getHandSize(t.opp)
        t.d.submitYesNo(t.opp, false).error shouldBe null

        t.d.getHandSize(t.opp) shouldBe handBefore
        t.peaceCounters() shouldBe 0
        t.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        t.d.declareAttackers(t.opp, mapOf(bears to t.me)).error shouldBe null
    }

    test("it triggers on its controller's own turn too") {
        val t = table()
        val bears = t.bears(t.me)
        t.toCombatPrompt(t.me)
        t.d.submitYesNo(t.me, true).error shouldBe null
        t.peaceCounters() shouldBe 1
        t.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        t.d.declareAttackers(t.me, mapOf(bears to t.opp)).error shouldNotBe null
    }

    test("twelve peace counters win the game at its controller's upkeep") {
        val t = table()
        // Past the opponent's combat, so its may-draw can't add a counter of its own.
        t.d.passPriorityUntil(Step.END, activePlayer = t.opp)
        t.d.replaceState(t.d.state.updateEntity(t.peace) { it.with(CountersComponent(mapOf(CounterType.PEACE to 12))) })
        t.d.passPriorityUntil(Step.UPKEEP, activePlayer = t.me)
        var guard = 0
        while (guard++ < 5 && !t.d.state.gameOver && t.d.stackSize > 0) t.d.bothPass()
        t.d.state.gameOver shouldBe true
        t.d.state.winnerId shouldBe t.me
    }

    test("eleven peace counters don't trigger the win") {
        val t = table()
        // Past the opponent's combat, so its may-draw can't add a counter of its own.
        t.d.passPriorityUntil(Step.END, activePlayer = t.opp)
        t.d.replaceState(t.d.state.updateEntity(t.peace) { it.with(CountersComponent(mapOf(CounterType.PEACE to 11))) })
        t.d.passPriorityUntil(Step.UPKEEP, activePlayer = t.me)
        t.d.stackSize shouldBe 0
        t.d.state.gameOver shouldBe false
    }
})
