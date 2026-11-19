package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.AcolytesReward
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Acolyte's Reward (BNG) — {1}{W} Instant. "Prevent the next X damage that would be dealt to target
 * creature this turn, where X is your devotion to white. If damage is prevented this way, Acolyte's
 * Reward deals that much damage to any target."
 *
 * Pins the rulings: X is locked in on resolution, the reaction is part of the prevention (no stack),
 * excess damage gets through, a partly spent shield keeps covering later damage, a departed second
 * target takes nothing while the damage is still prevented, and simultaneous combat damage from
 * several blockers is one prevention with one reaction.
 */
class AcolytesRewardScenarioTest : FunSpec({

    class Setup(val d: GameTestDriver, val me: EntityId, val opp: EntityId)

    fun resolveAll(d: GameTestDriver) {
        var guard = 0
        while (d.stackSize > 0 && d.pendingDecision == null && guard++ < 10) d.bothPass()
    }

    /** I control [lions] Savannah Lions ({W} each — that many devotion to white). */
    fun setUp(lions: Int): Pair<Setup, List<EntityId>> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + AcolytesReward)
            initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val s = Setup(d, me, d.getOpponent(me))
        return s to List(lions) { d.putCreatureOnBattlefield(me, "Savannah Lions") }
    }

    fun Setup.castReward(protect: EntityId, victim: EntityId) {
        val reward = d.putCardInHand(me, "Acolyte's Reward")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpell(me, reward, listOf(protect, victim)).error shouldBe null
        resolveAll(d)
    }

    fun Setup.opponentBolts(target: EntityId) {
        val bolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.passPriority(me)
        d.castSpell(opp, bolt, listOf(target)).error shouldBe null
        resolveAll(d)
    }

    fun Setup.damageOn(id: EntityId) = d.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("damage prevented to the creature is dealt by Acolyte's Reward to the second target") {
        val (s, lions) = setUp(lions = 3)
        val protectedLion = lions.first()
        s.castReward(protectedLion, s.opp)

        s.opponentBolts(protectedLion)

        withClue("devotion 3 prevents all of the Bolt") { s.damageOn(protectedLion) shouldBe 0 }
        withClue("that much — 3 — dealt to the opponent") { s.d.getLifeTotal(s.opp) shouldBe 17 }
        withClue("the reaction is part of the prevention: nothing waits on the stack") {
            s.d.stackSize shouldBe 0
            s.d.pendingDecision shouldBe null
        }
    }

    test("excess damage gets through and only the prevented amount is dealt") {
        val (s, lions) = setUp(lions = 2)
        val courser = s.d.putCreatureOnBattlefield(s.me, "Centaur Courser")
        s.castReward(courser, s.opp)

        s.opponentBolts(courser)

        withClue("devotion 2 prevents 2 of 3") { s.damageOn(courser) shouldBe 1 }
        s.d.getLifeTotal(s.opp) shouldBe 18
        lions.size shouldBe 2
    }

    test("a partly spent shield keeps covering later damage this turn") {
        val (s, lions) = setUp(lions = 4)
        val protectedLion = lions.first()
        s.castReward(protectedLion, s.opp)

        s.opponentBolts(protectedLion)
        s.d.getLifeTotal(s.opp) shouldBe 17

        s.opponentBolts(protectedLion)
        withClue("the last 1 of the shield prevents 1 of the second Bolt; the Lion dies to the rest") {
            s.d.getLifeTotal(s.opp) shouldBe 16
            s.d.getGraveyard(s.me).contains(protectedLion) shouldBe true
        }
    }

    test("a second target that has left the battlefield takes nothing; the damage is still prevented") {
        val (s, lions) = setUp(lions = 3)
        val protectedLion = lions.first()
        val force = s.d.putCreatureOnBattlefield(s.opp, "Force of Nature")
        s.castReward(protectedLion, force)
        s.d.moveToGraveyard(force)
        val returned = s.d.putCreatureOnBattlefield(s.opp, "Force of Nature")

        s.opponentBolts(protectedLion)

        s.damageOn(protectedLion) shouldBe 0
        withClue("a new Force is a new object the reaction can't name") { s.damageOn(returned) shouldBe 0 }
        s.d.getLifeTotal(s.opp) shouldBe 20
    }

    test("combat damage from two blockers is one prevention and one reaction of the total") {
        val (s, lions) = setUp(lions = 4)
        val attacker = lions.first()
        s.d.removeSummoningSickness(attacker)
        val courser = s.d.putCreatureOnBattlefield(s.opp, "Centaur Courser")
        val oppLion = s.d.putCreatureOnBattlefield(s.opp, "Savannah Lions")
        s.castReward(attacker, s.opp)

        s.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        s.d.declareAttackers(s.me, listOf(attacker), s.opp).error shouldBe null
        s.d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        s.d.declareBlockers(s.opp, mapOf(courser to listOf(attacker), oppLion to listOf(attacker))).error shouldBe null
        val seen = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        var guard = 0
        while (s.d.state.step != Step.POSTCOMBAT_MAIN && guard++ < 20) {
            if (s.d.pendingDecision is CombatResolutionDecision) s.d.confirmCombatDamage() else s.d.passPriority(s.d.state.priorityPlayerId!!)
            seen += s.d.events
        }

        s.d.pendingDecision shouldBe null
        withClue("3 + 1 from the blockers, all within devotion 4, all prevented") {
            s.d.getGraveyard(s.me).contains(attacker) shouldBe false
            s.damageOn(attacker) shouldBe 0
        }
        withClue("Acolyte's Reward deals the 4 prevented to the opponent") { s.d.getLifeTotal(s.opp) shouldBe 16 }
        withClue("as one damage event of 4, not one per blocker (3 and 1)") {
            seen.filterIsInstance<DamageDealtEvent>().filter { it.targetId == s.opp }.map { it.amount }.distinct() shouldBe listOf(4)
        }
    }
})
