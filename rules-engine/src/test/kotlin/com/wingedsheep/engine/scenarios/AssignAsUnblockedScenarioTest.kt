package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.combat.CombatRemovalHelper
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AssignAsUnblockedScenarioTest : FunSpec({
    fun attacker(name: String, keywords: Set<Keyword> = emptySet(), power: Int = 4) = CardDefinition.creature(
        name = name, subtypes = emptySet(), manaCost = ManaCost.parse("{3}{G}{G}"), power = power, toughness = 8,
        keywords = keywords, script = CardScript(staticAbilities = listOf(AssignCombatDamageAsUnblocked())),
    )
    fun blocker(name: String, banding: Boolean) = CardDefinition.creature(
        name = name, subtypes = emptySet(), manaCost = ManaCost.parse("{1}"), power = 1, toughness = 3,
        keywords = if (banding) setOf(Keyword.BANDING) else emptySet(),
    )
    data class Combat(val driver: GameTestDriver, val attacker: EntityId, val blockers: List<EntityId>)
    fun setup(banding: Boolean = false, count: Int = 1, doubleStrike: Boolean = false, power: Int = 4, protection: Boolean = false): Combat {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(listOf(
            attacker("Bypass Attacker", if (doubleStrike) setOf(Keyword.DOUBLE_STRIKE) else emptySet(), power),
            blocker("Banding Blocker", true), blocker("Plain Blocker", false),
        ))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val a = d.putCreatureOnBattlefield(d.player1, "Bypass Attacker")
        d.removeSummoningSickness(a)
        val blockers = (0 until count).map { index ->
            d.putCreatureOnBattlefield(d.player2, if (protection) "Voice of Duty" else if (banding && index == 0) "Banding Blocker" else "Plain Blocker")
        }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(a), d.player2)
        d.bothPass()
        d.declareBlockers(d.player2, blockers.associateWith { listOf(a) })
        val order = d.pendingDecision
        if (order is OrderObjectsDecision) d.submitDecision(order.playerId, OrderedResponse(order.id, blockers))
        return Combat(d, a, blockers)
    }
    fun damage(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    for (emptyList in listOf(false, true)) {
        for (accept in listOf(false, true)) {
            test("last blocker gone, empty list=$emptyList, bypass=$accept") {
                val (d, a, blockers) = setup()
                if (emptyList) d.replaceState(CombatRemovalHelper.removeFromCombat(d.state, blockers.single()))
                d.moveToGraveyard(blockers.single())
                d.state.getEntity(a)!!.has<BlockedComponent>() shouldBe true
                d.bothPass()
                d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player1
                d.submitYesNo(d.player1, accept).error shouldBe null
                d.pendingDecision shouldBe null
                d.getLifeTotal(d.player2) shouldBe if (accept) 16 else 20
                damage(d, a) shouldBe 0
            }
        }
    }
    for (accept in listOf(false, true)) {
        test("banding defender chooses bypass=$accept and attacker cannot answer") {
            val (d, a, blockers) = setup(banding = true)
            d.bothPass()
            val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            decision.playerId shouldBe d.player2
            (d.submitYesNo(d.player1, !accept).error != null) shouldBe true
            d.pendingDecision shouldBe decision
            d.submitYesNo(d.player2, accept).error shouldBe null
            d.getLifeTotal(d.player2) shouldBe if (accept) 16 else 20
            (blockers.single() in d.state.getBattlefield()) shouldBe accept
            damage(d, a) shouldBe 1
        }
    }
    test("a departed banding blocker no longer controls the choice") {
        val (d, _, blockers) = setup(banding = true, count = 2)
        d.moveToGraveyard(blockers.first())
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player1
        d.submitYesNo(d.player1, true).error shouldBe null
        d.getLifeTotal(d.player2) shouldBe 16
    }
    for (banding in listOf(false, true)) {
        test("declining preserves manual blocker allocation, banding=$banding") {
            val (d, a, blockers) = setup(banding = banding, count = 2)
            d.bothPass()
            val chooser = if (banding) d.player2 else d.player1
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe chooser
            d.submitYesNo(chooser, false).error shouldBe null
            val board = d.pendingDecision.shouldBeInstanceOf<CombatResolutionDecision>()
            board.playerId shouldBe chooser
            // Banding permits spreading below lethal. Without it choose all damage to the first blocker.
            val amounts = board.edges.map { edge ->
                DamageEdgeAmount(edge.id, if (edge.sourceId == a) {
                    if (banding) 2 else if (edge.targetId == blockers.first()) 4 else 0
                } else edge.amount)
            }
            d.submitDecision(chooser, CombatResolutionResponse(board.id, amounts)).error shouldBe null
            d.pendingDecision shouldBe null
            d.getLifeTotal(d.player2) shouldBe 20
            if (banding) blockers.forEach { damage(d, it) shouldBe 2 }
            else (blockers.first() in d.state.getBattlefield()) shouldBe false
            damage(d, a) shouldBe 2
        }
    }
    test("double strike asks afresh after first strike kills the sole blocker") {
        val (d, _, blockers) = setup(doubleStrike = true)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, false).error shouldBe null
        (blockers.single() in d.state.getBattlefield()) shouldBe false
        d.getLifeTotal(d.player2) shouldBe 20
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.getLifeTotal(d.player2) shouldBe 16
    }
    test("protection from the attacker does not prevent assigning as unblocked") {
        val (d, a, blockers) = setup(protection = true)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.getLifeTotal(d.player2) shouldBe 16
        (blockers.single() in d.state.getBattlefield()) shouldBe true
        damage(d, blockers.single()) shouldBe 0
        damage(d, a) shouldBe 2
    }
    test("banding defender may assign the entire bypass to the attacked planeswalker") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(listOf(attacker("Bypass Attacker"), blocker("Banding Blocker", true),
            CardDefinition.planeswalker("Test Walker", ManaCost.parse("{4}"), emptySet(), 8)))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val a = d.putCreatureOnBattlefield(d.player1, "Bypass Attacker")
        d.removeSummoningSickness(a)
        val b = d.putCreatureOnBattlefield(d.player2, "Banding Blocker")
        val walker = d.putPermanentOnBattlefield(d.player2, "Test Walker")
        d.addComponent(walker, CountersComponent(mapOf(CounterType.LOYALTY to 8)))
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(a), walker).error shouldBe null
        d.bothPass()
        d.declareBlockers(d.player2, mapOf(b to listOf(a))).error shouldBe null
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player2
        d.submitYesNo(d.player2, true).error shouldBe null
        d.getLifeTotal(d.player2) shouldBe 20
        d.state.getEntity(walker)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
        damage(d, a) shouldBe 1
        damage(d, b) shouldBe 0
    }
    test("zero damage never asks a bypass question") {
        val (d, _, _) = setup(power = 0)
        d.bothPass()
        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player2) shouldBe 20
    }
    test("multiple bypass attackers resume each question before dealing simultaneous damage") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(listOf(attacker("Bypass Attacker"), blocker("Plain Blocker", false)))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attackers = (1..2).map { d.putCreatureOnBattlefield(d.player1, "Bypass Attacker").also(d::removeSummoningSickness) }
        val blockers = (1..2).map { d.putCreatureOnBattlefield(d.player2, "Plain Blocker") }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, attackers, d.player2)
        d.bothPass()
        d.declareBlockers(d.player2, blockers.zip(attackers).associate { (b, a) -> b to listOf(a) })
        d.bothPass()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.getLifeTotal(d.player2) shouldBe 20
        d.submitYesNo(d.player1, true).error shouldBe null
        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player2) shouldBe 12
        attackers.forEach { damage(d, it) shouldBe 1 }
    }
})
