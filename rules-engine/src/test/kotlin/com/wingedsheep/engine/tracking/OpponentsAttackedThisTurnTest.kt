package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.PlayerAttackedPlayersThisTurnComponent
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OpponentsAttackedThisTurnTest : FunSpec({
    val walker = card("Defender Walker Probe") { typeLine = "Planeswalker — Test"; startingLoyalty = 20 }
    val battle = card("Defender Battle Probe") { typeLine = "Battle — Siege"; startingDefense = 20 }
    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(walker, battle))
        initMultiplayer(List(4) { Deck.of("Forest" to 40) })
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val evaluator = PredicateEvaluator(cardRegistry = null)
    fun count(d: GameTestDriver, player: Player = Player.You): Int = evaluator.amounts.evaluate(
        d.state, DynamicAmounts.opponentsAttackedThisTurn(player), EffectContext(null, d.player1),
    )
    fun attacker(d: GameTestDriver): EntityId = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also {
        d.removeSummoningSickness(it)
    }
    fun attack(d: GameTestDriver, assignments: Map<EntityId, EntityId>) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, assignments).error shouldBe null
    }
    test("multiple creatures against one player count once and the count belongs to the attacking player") {
        val d = setup()
        attack(d, mapOf(attacker(d) to d.player2, attacker(d) to d.player2))
        count(d) shouldBe 1
        count(d, Player.EachOpponent) shouldBe 0
        count(d, Player.Each) shouldBe 1
    }
    test("distinct player defenders count separately") {
        val d = setup()
        attack(d, mapOf(attacker(d) to d.player2, attacker(d) to d.state.turnOrder[2]))
        count(d) shouldBe 2
    }
    test("planeswalkers and battles enable Raid but do not count as attacking their defenders") {
        val d = setup()
        val pw = d.putPermanentOnBattlefield(d.player2, walker.name)
        d.addComponent(pw, CountersComponent().withAdded(CounterType.LOYALTY, 20))
        val b = d.putPermanentOnBattlefield(d.player1, battle.name)
        d.addComponent(b, CountersComponent().withAdded(CounterType.DEFENSE, 20))
        d.addComponent(b, ProtectorComponent(d.state.turnOrder[2]))
        attack(d, mapOf(attacker(d) to pw, attacker(d) to b))
        count(d) shouldBe 0
        evaluator.conditions.evaluate(d.state, Conditions.YouAttackedThisTurn, EffectContext(null, d.player1)) shouldBe true
        evaluator.conditions.evaluate(d.state, Conditions.PlayerAttackedPlayerThisTurn(Player.You, Player.EachOpponent), EffectContext(null, d.player1)) shouldBe false
    }
    test("a creature entering attacking is not a declared attack") {
        val d = setup()
        val id = attacker(d)
        d.addComponent(id, com.wingedsheep.engine.state.components.combat.AttackingComponent(d.player2))
        count(d) shouldBe 0
    }
    test("an empty declaration records zero") {
        val d = setup()
        attacker(d)
        attack(d, emptyMap())
        count(d) shouldBe 0
    }
    test("extra combats union defenders and cleanup clears the history") {
        val d = setup()
        d.addComponent(d.player1, AdditionalPhasesComponent(listOf(QueuedPhase(ExtraPhaseKind.COMBAT))))
        attack(d, mapOf(attacker(d) to d.player2))
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        attack(d, mapOf(attacker(d) to d.player2, attacker(d) to d.state.turnOrder[2]))
        count(d) shouldBe 2
        d.passPriorityUntil(Step.UPKEEP)
        count(d) shouldBe 0
        d.state.getEntity(d.player1)?.has<PlayerAttackedPlayersThisTurnComponent>() shouldBe false
    }
    test("departures do not erase the historical count") {
        val d = setup()
        attack(d, mapOf(attacker(d) to d.player2))
        d.submit(com.wingedsheep.engine.core.Concede(d.player2)).error shouldBe null
        count(d) shouldBe 1
    }
})
