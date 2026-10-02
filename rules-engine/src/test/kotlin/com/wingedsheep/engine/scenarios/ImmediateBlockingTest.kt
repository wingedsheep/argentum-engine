package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImmediateBlockingTest : FunSpec({
    val blockerCard = card("Immediate Blocker") {
        typeLine = "Creature"; power = 1; toughness = 4
        triggeredAbility { trigger = Triggers.self.blocks(); effect = Effects.GainLife(1) }
        triggeredAbility { trigger = Triggers.self.blocks(GameObjectFilter.Creature); effect = Effects.GainLife(2) }
        triggeredAbility { trigger = Triggers.self.blocks(minBlockedAttackers = 2); effect = Effects.GainLife(3) }
    }
    val attackerCard = card("Immediate Attacker") {
        typeLine = "Creature"; power = 1; toughness = 4
        triggeredAbility { trigger = Triggers.self.becomesBlocked(); effect = Effects.GainLife(1) }
        triggeredAbility { trigger = Triggers.self.becomesBlocked(GameObjectFilter.Creature); effect = Effects.GainLife(2) }
        triggeredAbility { trigger = Triggers.self.attacksAndIsntBlocked(); effect = Effects.GainLife(4) }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(blockerCard, attackerCard))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.attack(defender: EntityId, band: String? = null): EntityId =
        putCreatureOnBattlefield(activePlayer!!, attackerCard.name).also { id ->
            addComponent(id, AttackingComponent(defender, band))
        }
    fun GameTestDriver.block(blocker: EntityId, attacker: EntityId): EffectResult {
        val result = services.effectExecutorRegistry.execute(state,
            Effects.BecomeBlocking(EffectTarget.ContextTarget(0), EffectTarget.ContextTarget(1)),
            EffectContext(sourceId = null, controllerId = activePlayer!!,
                targets = listOf(ChosenTarget.Permanent(blocker), ChosenTarget.Permanent(attacker))))
        result.error shouldBe null
        replaceState(result.state)
        return result
    }
    fun GameTestDriver.remove(id: EntityId, unblock: Boolean = false): EffectResult {
        val result = services.effectExecutorRegistry.execute(state,
            Effects.RemoveFromCombat(EffectTarget.ContextTarget(0), unblock),
            EffectContext(sourceId = null, controllerId = activePlayer!!, targets = listOf(ChosenTarget.Permanent(id))))
        replaceState(result.state); return result
    }
    test("adding another attacker preserves existing pairs and both history axes") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attack(opp); val b = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.block(blocker, a); val result = d.block(blocker, b)
        d.state.getEntity(blocker)!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe listOf(a, b)
        d.state.getEntity(blocker)!!.get<CombatPartnersThisTurnComponent>()!!.partnerIds shouldBe setOf(a, b)
        d.state.getEntity(b)!!.get<BlockersThisCombatComponent>()!!.blockers shouldBe setOf(d.state.objectRef(blocker))
        (result.events.single() as BlocksCreatedEvent).newBlockers shouldBe emptySet()
    }
    test("an already present pair is a no-op with no duplicate event") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!); val a = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.block(blocker, a); val before = d.state
        val result = d.block(blocker, a)
        result.state shouldBe before; result.events shouldBe emptyList()
    }
    test("new partner fires per-creature and threshold triggers without repeating generic blocks") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attack(opp); val b = d.attack(opp); val c = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        val first = d.block(blocker, a)
        d.services.triggerDetector.detectTriggers(d.state, first.events).count { it.sourceId == blocker } shouldBe 2
        val second = d.block(blocker, b)
        d.services.triggerDetector.detectTriggers(d.state, second.events).count { it.sourceId == blocker } shouldBe 2
        val third = d.block(blocker, c)
        d.services.triggerDetector.detectTriggers(d.state, third.events).count { it.sourceId == blocker } shouldBe 1
    }
    test("already blocked attacker fires only the new-blocker trigger") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!); val a = d.attack(opp)
        val b = d.putCreatureOnBattlefield(opp, blockerCard.name); val c = d.putCreatureOnBattlefield(opp, blockerCard.name)
        val first = d.block(b, a)
        d.services.triggerDetector.detectTriggers(d.state, first.events).count { it.sourceId == a } shouldBe 2
        val second = d.block(c, a)
        d.services.triggerDetector.detectTriggers(d.state, second.events).count { it.sourceId == a } shouldBe 1
        (second.events.single() as BlocksCreatedEvent).newlyBlockedAttackers shouldBe emptySet()
    }
    test("effect-created block does not signal unblocked attackers elsewhere") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!); val a = d.attack(opp); val b = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        val result = d.block(blocker, a)
        d.services.triggerDetector.detectTriggers(d.state, result.events).count { it.sourceId == b } shouldBe 0
    }
    test("a block created against one band member blocks the entire band") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attack(opp, "band"); val b = d.attack(opp, "band")
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.block(blocker, a)
        d.state.getEntity(blocker)!!.get<BlockingComponent>()!!.blockedAttackerIds.toSet() shouldBe setOf(a, b)
        d.state.getEntity(b)!!.get<BlockedComponent>()!!.blockerIds shouldBe listOf(blocker)
    }
    test("normal combat removal preserves blocked status and emits a notification") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!); val a = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.block(blocker, a); val result = d.remove(blocker)
        d.state.getEntity(a)!!.get<BlockedComponent>()!!.blockerIds shouldBe emptyList()
        result.events shouldBe listOf(RemovedFromCombatEvent(blocker))
        d.remove(blocker).events shouldBe emptyList()
    }
    test("repeated pair with the same object still permits the explicit sole-blocker exception") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!); val a = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.block(blocker, a); d.remove(blocker); d.block(blocker, a); d.remove(blocker, true)
        d.state.getEntity(a)!!.has<BlockedComponent>() shouldBe false
    }
    test("combat-end clears per-combat identity history while retaining turn partners") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!); val a = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name); d.block(blocker, a)
        d.replaceState(d.services.combatManager.endCombat(d.state).state)
        d.state.getEntity(a)!!.has<BlockersThisCombatComponent>() shouldBe false
        d.state.getEntity(a)!!.get<CombatPartnersThisTurnComponent>()!!.partnerIds shouldBe setOf(blocker)
    }
    test("attacker selection and execution refuse the wrong defending player in multiplayer") {
        val d = GameTestDriver(); d.registerCards(TestCards.all + listOf(blockerCard, attackerCard))
        val players = d.initMultiplayer(List(3) { Deck.of("Forest" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val blocker = d.putCreatureOnBattlefield(players[1], blockerCard.name)
        val a = d.attack(players[1]); val b = d.attack(players[2])
        val filter = GameObjectFilter.Creature.attackingDefenderOf(EffectTarget.ContextTarget(0))
        val context = PredicateContext(controllerId = players[0], targets = listOf(ChosenTarget.Permanent(blocker)))
        d.services.predicateEvaluator.matches(d.state, d.state.projectedState, a, filter, context) shouldBe true
        d.services.predicateEvaluator.matches(d.state, d.state.projectedState, b, filter, context) shouldBe false
        d.block(blocker, b).events shouldBe emptyList()
        d.block(blocker, a).events.size shouldBe 1
    }
    test("a creature on the attacking side cannot also become a blocker") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!); val a = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.addComponent(blocker, AttackingComponent(d.activePlayer!!))
        d.block(blocker, a).events shouldBe emptyList()
    }
    test("nonattacking creature is not made blocked") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.putCreatureOnBattlefield(d.activePlayer!!, attackerCard.name)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.block(blocker, a).events shouldBe emptyList()
    }
    test("swapped blockers preserve both sides and history without repeating status triggers") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attack(opp); val b = d.attack(opp)
        val first = d.putCreatureOnBattlefield(opp, blockerCard.name)
        val second = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.block(first, a); d.block(second, b)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            com.wingedsheep.sdk.scripting.effects.SwapBlockingAssignmentsEffect,
            EffectContext(sourceId = null, controllerId = d.activePlayer!!,
                targets = listOf(ChosenTarget.Permanent(first), ChosenTarget.Permanent(second))))
        result.error shouldBe null; d.replaceState(result.state)
        d.state.getEntity(a)!!.get<BlockedComponent>()!!.blockerIds shouldBe listOf(second)
        d.state.getEntity(b)!!.get<BlockedComponent>()!!.blockerIds shouldBe listOf(first)
        d.state.getEntity(first)!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe listOf(b)
        d.state.getEntity(a)!!.get<BlockersThisCombatComponent>()!!.blockers shouldBe
            setOf(d.state.objectRef(first), d.state.objectRef(second))
        d.services.triggerDetector.detectTriggers(d.state, result.events).count { it.sourceId == first } shouldBe 1
        d.services.triggerDetector.detectTriggers(d.state, result.events).count { it.sourceId == a } shouldBe 1
        d.remove(second, true)
        d.state.getEntity(a)!!.has<BlockedComponent>() shouldBe true
    }

    test("planeswalker controller and battle protector determine the defending side") {
        val walker = card("Immediate Walker") { typeLine = "Planeswalker"; startingLoyalty = 4 }
        val battle = card("Immediate Battle") { typeLine = "Battle — Siege"; startingDefense = 4 }
        val d = driver(); d.registerCards(listOf(walker, battle)); val opp = d.getOpponent(d.activePlayer!!)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        val w = d.putPermanentOnBattlefield(opp, walker.name)
        val siege = d.putPermanentOnBattlefield(d.activePlayer!!, battle.name)
        d.addComponent(siege, com.wingedsheep.engine.state.components.battlefield.ProtectorComponent(opp))
        val a = d.attack(w); val b = d.attack(siege)
        val filter = GameObjectFilter.Creature.attackingDefenderOf(EffectTarget.ContextTarget(0))
        val context = PredicateContext(controllerId = d.activePlayer!!, targets = listOf(ChosenTarget.Permanent(blocker)))
        d.services.predicateEvaluator.matches(d.state, d.state.projectedState, a, filter, context) shouldBe true
        d.services.predicateEvaluator.matches(d.state, d.state.projectedState, b, filter, context) shouldBe true
        d.block(blocker, a).events.size shouldBe 1
        d.block(blocker, b).events.size shouldBe 1
    }
    test("shared-turn teammates can block attacks defended by the other teammate") {
        val d = GameTestDriver(); d.registerCards(TestCards.all + listOf(blockerCard, attackerCard))
        val players = d.initMultiplayer(List(4) { Deck.of("Forest" to 40) },
            format = com.wingedsheep.sdk.core.Format.TwoHeadedGiant(), teams = listOf(listOf(0, 1), listOf(2, 3)))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val blocker = d.putCreatureOnBattlefield(players[3], blockerCard.name); val a = d.attack(players[2])
        val filter = GameObjectFilter.Creature.attackingDefenderOf(EffectTarget.ContextTarget(0))
        val context = PredicateContext(controllerId = players[0], targets = listOf(ChosenTarget.Permanent(blocker)))
        d.services.predicateEvaluator.matches(d.state, d.state.projectedState, a, filter, context) shouldBe true
        d.block(blocker, a).events.size shouldBe 1
    }
    test("defender-relative selection and blocking read the referenced creature's projected controller") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val blocker = d.putCreatureOnBattlefield(me, blockerCard.name); val a = d.attack(opp)
        val control = d.services.effectExecutorRegistry.execute(d.state,
            Effects.GainControl(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = null, controllerId = opp, targets = listOf(ChosenTarget.Permanent(blocker))))
        control.error shouldBe null; d.replaceState(control.state)
        d.state.projectedState.getController(blocker) shouldBe opp
        val filter = GameObjectFilter.Creature.attackingDefenderOf(EffectTarget.ContextTarget(0))
        val context = PredicateContext(controllerId = me, targets = listOf(ChosenTarget.Permanent(blocker)))
        d.services.predicateEvaluator.matches(d.state, d.state.projectedState, a, filter, context) shouldBe true
        d.block(blocker, a).events.size shouldBe 1
    }

})
