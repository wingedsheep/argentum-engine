package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Unsummon
import com.wingedsheep.mtg.sets.definitions.neo.cards.MoonCircuitHacker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.ninjutsuAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Moon-Circuit Hacker — {1}{U} Enchantment Creature — Human Ninja 2/1
 * Ninjutsu {U}
 * Whenever this creature deals combat damage to a player, you may draw a card. If you do, discard
 * a card unless this creature entered this turn.
 *
 * "Entered this turn" is read from last-known information when the Hacker has left the battlefield
 * before its trigger resolves (CR 608.2h).
 */
class MoonCircuitHackerScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + MoonCircuitHacker + Unsummon)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Resolve the stack, answering "may" with [yes] and recording every discard prompt. */
    fun settle(d: GameTestDriver, yes: Boolean): Int {
        var discards = 0
        var guard = 0
        while (guard++ < 20 && (d.state.stack.isNotEmpty() || d.pendingDecision != null)) {
            when (val decision = d.pendingDecision) {
                null -> d.bothPass()
                is YesNoDecision -> d.submitYesNo(decision.playerId, yes).error shouldBe null
                is SelectCardsDecision -> {
                    discards++
                    d.submitCardSelection(decision.playerId, decision.options.take(1)).error shouldBe null
                }
                else -> error("unexpected decision $decision")
            }
        }
        return discards
    }

    fun attackUnblocked(d: GameTestDriver, attacker: EntityId) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        var guard = 0
        while (d.state.priorityPlayerId != d.player1 && d.state.step == Step.DECLARE_BLOCKERS && guard++ < 4) {
            d.passPriority(d.state.priorityPlayerId!!)
        }
    }

    /** Ninjutsu the Hacker in for an unblocked Grizzly Bears, so it entered the battlefield this turn. */
    fun ninjutsuIn(d: GameTestDriver): EntityId {
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also(d::removeSummoningSickness)
        attackUnblocked(d, bears)
        val hacker = d.putCardInHand(d.player1, MoonCircuitHacker.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.submit(
            ActivateAbility(
                playerId = d.player1,
                sourceId = hacker,
                abilityId = ninjutsuAbility("{U}").id,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(bears)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        while (d.state.stack.isNotEmpty()) d.bothPass()
        return d.findPermanent(d.player1, MoonCircuitHacker.name)!!
    }

    test("ninjutsu'd in this turn: the draw is not followed by a discard") {
        val d = driver()
        ninjutsuIn(d)
        val handBefore = d.getHandSize(d.player1)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)

        settle(d, yes = true) shouldBe 0
        d.getLifeTotal(d.player2) shouldBe 18
        d.getHandSize(d.player1) shouldBe handBefore + 1
    }

    test("on the battlefield since an earlier turn: draw, then discard") {
        val d = driver()
        val hacker = d.putCreatureOnBattlefield(d.player1, MoonCircuitHacker.name).also(d::removeSummoningSickness)
        val handBefore = d.getHandSize(d.player1)
        attackUnblocked(d, hacker)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)

        settle(d, yes = true) shouldBe 1
        d.getHandSize(d.player1) shouldBe handBefore
    }

    test("bounced in response after entering this turn: last-known info still skips the discard") {
        val d = driver()
        val hacker = ninjutsuIn(d)
        val unsummon = d.putCardInHand(d.player1, Unsummon.name)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        d.state.stack.size shouldBe 1
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, unsummon, listOf(hacker)).outcome shouldBe Outcome.Done
        d.bothPass() // Unsummon resolves; the Hacker's trigger is still on the stack
        d.getHand(d.player1) shouldContain hacker
        val handBefore = d.getHandSize(d.player1)

        settle(d, yes = true) shouldBe 0
        d.getHandSize(d.player1) shouldBe handBefore + 1
    }

    test("bounced in response after an earlier turn: last-known info still demands the discard") {
        val d = driver()
        val hacker = d.putCreatureOnBattlefield(d.player1, MoonCircuitHacker.name).also(d::removeSummoningSickness)
        val unsummon = d.putCardInHand(d.player1, Unsummon.name)
        attackUnblocked(d, hacker)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, unsummon, listOf(hacker)).outcome shouldBe Outcome.Done
        d.bothPass()
        val handBefore = d.getHandSize(d.player1)

        settle(d, yes = true) shouldBe 1
        d.getHandSize(d.player1) shouldBe handBefore
    }

    test("declining the draw skips the discard too") {
        val d = driver()
        val hacker = d.putCreatureOnBattlefield(d.player1, MoonCircuitHacker.name).also(d::removeSummoningSickness)
        val handBefore = d.getHandSize(d.player1)
        attackUnblocked(d, hacker)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)

        settle(d, yes = false) shouldBe 0
        d.getHandSize(d.player1) shouldBe handBefore
    }
})
