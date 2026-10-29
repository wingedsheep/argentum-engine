package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.BlocksCreatedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Put onto the battlefield blocking" — `BecomeBlocking(entering = true)` (CR 509.4): the creature is
 * blocking, but for trigger events and effects it never "blocked" (CR 509.3a-b), while the attacker
 * still becomes blocked / becomes blocked by it (CR 509.3c-d).
 */
class EntersBlockingTest : FunSpec({
    val blockerCard = card("Entering Blocker") {
        typeLine = "Creature"; power = 1; toughness = 4
        triggeredAbility { trigger = Triggers.self.blocks(); effect = Effects.GainLife(1) }
        triggeredAbility { trigger = Triggers.self.blocks(GameObjectFilter.Creature); effect = Effects.GainLife(2) }
        triggeredAbility { trigger = Triggers.self.blocksOrBecomesBlocked(); effect = Effects.GainLife(3) }
    }
    val attackerCard = card("Entering-Blocked Attacker") {
        typeLine = "Creature"; power = 1; toughness = 4
        triggeredAbility { trigger = Triggers.self.becomesBlocked(); effect = Effects.GainLife(1) }
        triggeredAbility { trigger = Triggers.self.becomesBlocked(GameObjectFilter.Creature); effect = Effects.GainLife(2) }
        triggeredAbility { trigger = Triggers.self.blocksOrBecomesBlocked(); effect = Effects.GainLife(3) }
    }
    val watcherCard = card("Block Watcher") {
        typeLine = "Creature"; power = 1; toughness = 1
        triggeredAbility { trigger = Triggers.a(GameObjectFilter.Creature).blocks(); effect = Effects.GainLife(1) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(blockerCard, attackerCard, watcherCard))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.attack(defender: EntityId): EntityId =
        putCreatureOnBattlefield(activePlayer!!, attackerCard.name).also { addComponent(it, AttackingComponent(defender)) }
    fun GameTestDriver.block(blocker: EntityId, attacker: EntityId, entering: Boolean): EffectResult {
        val result = services.effectExecutorRegistry.execute(state,
            Effects.BecomeBlocking(EffectTarget.ContextTarget(0), EffectTarget.ContextTarget(1), entering),
            EffectContext(sourceId = null, controllerId = activePlayer!!,
                targets = listOf(ChosenTarget.Permanent(blocker), ChosenTarget.Permanent(attacker))))
        result.error shouldBe null
        replaceState(result.state)
        return result
    }
    fun GameTestDriver.triggersFrom(result: EffectResult, source: EntityId) =
        services.triggerDetector.detectTriggers(state, result.events).count { it.sourceId == source }

    test("entering blocker is blocking but its own blocks triggers don't fire (CR 509.3a-b)") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        val result = d.block(blocker, a, entering = true)

        d.state.getEntity(blocker)!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe listOf(a)
        d.state.getEntity(a)!!.get<BlockedComponent>()!!.blockerIds shouldBe listOf(blocker)
        val event = result.events.single() as BlocksCreatedEvent
        event.newBlockers shouldBe emptySet()
        event.enteredBlockers shouldBe setOf(blocker)
        d.triggersFrom(result, blocker) shouldBe 0
    }

    test("the attacker becomes blocked and becomes blocked by it (CR 509.3c-d)") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        val result = d.block(blocker, a, entering = true)
        // becomesBlocked, becomesBlocked(by Creature), blocksOrBecomesBlocked
        d.triggersFrom(result, a) shouldBe 3
    }

    test("an already blocked attacker fires only becomes-blocked-by, not becomes-blocked") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attack(opp)
        d.block(d.putCreatureOnBattlefield(opp, "Centaur Courser"), a, entering = false)
        val result = d.block(d.putCreatureOnBattlefield(opp, blockerCard.name), a, entering = true)
        val fired = d.services.triggerDetector.detectTriggers(d.state, result.events).filter { it.sourceId == a }
        fired.size shouldBe 1
        fired.single().ability.effect shouldBe Effects.GainLife(2)
    }

    test("\"whenever a creature blocks\" observers don't see it") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val watcher = d.putCreatureOnBattlefield(opp, watcherCard.name)
        val a = d.attack(opp)
        val result = d.block(d.putCreatureOnBattlefield(opp, "Centaur Courser"), a, entering = true)
        d.triggersFrom(result, watcher) shouldBe 0

        // Control: an ordinary effect-created block is seen.
        val b = d.attack(opp)
        val ordinary = d.block(d.putCreatureOnBattlefield(opp, "Centaur Courser"), b, entering = false)
        d.triggersFrom(ordinary, watcher) shouldBe 1
    }

    test("it never \"blocked\" for effects: no blocked-this-combat or blocked-this-turn history") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attack(opp)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        d.block(blocker, a, entering = true)
        val entity = d.state.getEntity(blocker)!!
        entity.has<BlockedThisCombatComponent>() shouldBe false
        entity.has<BlockedThisTurnComponent>() shouldBe false
        d.state.getEntity(a)!!.get<CombatPartnersThisTurnComponent>()!!.blockedByIds shouldContain blocker
    }

    test("a creature that is no longer attacking can't be blocked by the entering creature (CR 509.4a)") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.putCreatureOnBattlefield(d.activePlayer!!, attackerCard.name)
        val blocker = d.putCreatureOnBattlefield(opp, blockerCard.name)
        val result = d.block(blocker, a, entering = true)
        result.events shouldBe emptyList()
        d.state.getEntity(blocker)!!.has<BlockingComponent>() shouldBe false
    }

    test("the entering creature's controller must be a defending player (CR 509.4a)") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val a = d.attack(opp)
        val ownCreature = d.putCreatureOnBattlefield(me, blockerCard.name)
        val result = d.block(ownCreature, a, entering = true)
        result.events shouldBe emptyList()
        d.state.getEntity(ownCreature)!!.has<BlockingComponent>() shouldBe false
    }
})
