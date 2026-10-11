package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.CombatResolutionResponse
import com.wingedsheep.engine.core.DamageEdgeDirection
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The AI divides an attacker's combat damage among several blockers to kill what it can
 * (CR 510.1c: divided as its controller chooses — no damage-assignment order), rather than
 * echoing the engine's lethal-in-declaration-order default.
 *
 * Self-play repro (TLA vs POR): Knight Errant (2/2) was double-blocked by a Monk token pumped to
 * 4/4 and Yuyan Archers (4/2); the AI put both points on the 4/4 and killed nothing.
 */
class CombatDamageAssignmentAiTest : FunSpec({

    fun creature(name: String, power: Int, toughness: Int) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Human")),
        power = power,
        toughness = toughness,
    )

    val knight = creature("AI Test Knight Errant", 2, 2)
    val monk = creature("AI Test Monk", 4, 4)
    val archers = creature("AI Test Archers", 4, 2)

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(knight, monk, archers))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.advanceToCombatResolution(): CombatResolutionDecision {
        var passes = 0
        while (state.pendingDecision == null && passes++ < 50) submit(PassPriority(state.priorityPlayerId!!))
        return state.pendingDecision.shouldBeInstanceOf<CombatResolutionDecision>()
    }

    fun GameTestDriver.aiAnswer(decision: CombatResolutionDecision): CombatResolutionResponse {
        val responder = DecisionResponder(GameSimulator(cardRegistry), AIPlayer.defaultEvaluator())
        return responder.respond(state, decision, decision.playerId) as CombatResolutionResponse
    }

    fun CombatResolutionResponse.amountTo(decision: CombatResolutionDecision, source: EntityId, target: EntityId): Int {
        val edge = decision.edges.single { it.sourceId == source && it.targetId == target }
        return edges.single { it.edgeId == edge.id }.amount
    }

    test("2/2 double-blocked by a 4/4 and a 4/2 kills the 4/2 instead of chipping the 4/4") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, knight.name)
        val bigMonk = d.putCreatureOnBattlefield(opponent, monk.name)
        val archer = d.putCreatureOnBattlefield(opponent, archers.name)
        d.removeSummoningSickness(attacker)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(attacker), opponent)
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // The 4/4 is declared first, so the engine's default fill puts both points on it.
        d.declareBlockers(opponent, linkedMapOf(bigMonk to listOf(attacker), archer to listOf(attacker)))

        val decision = d.advanceToCombatResolution()
        decision.playerId shouldBe me
        decision.edges.single { it.sourceId == attacker && it.targetId == bigMonk }.amount shouldBe 2

        val answer = d.aiAnswer(decision)
        answer.amountTo(decision, attacker, archer) shouldBe 2
        answer.amountTo(decision, attacker, bigMonk) shouldBe 0

        d.submitDecision(me, answer).error shouldBe null
        d.findPermanent(opponent, archers.name) shouldBe null
        d.findPermanent(opponent, monk.name) shouldBe bigMonk
    }

    test("deathtouch trampler gives each blocker one point and tramples over the rest") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val trampler = d.putCreatureOnBattlefield(me, "Deathtouch Trampler")      // 3/3 deathtouch, trample
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")     // 3/3
        val lions = d.putCreatureOnBattlefield(opponent, "Savannah Lions")        // 1/1
        d.removeSummoningSickness(trampler)
        val lifeBefore = d.getLifeTotal(opponent)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(trampler), opponent)
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opponent, linkedMapOf(courser to listOf(trampler), lions to listOf(trampler)))

        val decision = d.advanceToCombatResolution()
        val answer = d.aiAnswer(decision)
        answer.amountTo(decision, trampler, courser) shouldBe 1
        answer.amountTo(decision, trampler, lions) shouldBe 1
        val drain = decision.edges.single { it.sourceId == trampler && it.isTrampleDrain }
        drain.direction shouldBe DamageEdgeDirection.ATTACKER_TO_PLAYER
        answer.edges.single { it.edgeId == drain.id }.amount shouldBe 1

        d.submitDecision(me, answer).error shouldBe null
        d.findPermanent(opponent, "Centaur Courser") shouldBe null
        d.findPermanent(opponent, "Savannah Lions") shouldBe null
        d.getLifeTotal(opponent) shouldBe lifeBefore - 1
    }

    test("a trampler that cannot bring every blocker to lethal sends nothing to the player and kills what it can") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val beast = d.putCreatureOnBattlefield(me, "Trample Beast")              // 5/5 trample
        val bigMonk = d.putCreatureOnBattlefield(opponent, monk.name)            // 4/4
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")    // 3/3
        d.removeSummoningSickness(beast)
        val lifeBefore = d.getLifeTotal(opponent)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(beast), opponent)
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opponent, linkedMapOf(courser to listOf(beast), bigMonk to listOf(beast)))

        val decision = d.advanceToCombatResolution()
        val answer = d.aiAnswer(decision)
        // 7 toughness across the blockers: no trample damage is legal, so kill the more valuable 4/4.
        answer.amountTo(decision, beast, bigMonk) shouldBe 4
        val drain = decision.edges.single { it.sourceId == beast && it.isTrampleDrain }
        answer.edges.single { it.edgeId == drain.id }.amount shouldBe 0

        d.submitDecision(me, answer).error shouldBe null
        d.findPermanent(opponent, monk.name) shouldBe null
        d.getLifeTotal(opponent) shouldBe lifeBefore
    }
})
