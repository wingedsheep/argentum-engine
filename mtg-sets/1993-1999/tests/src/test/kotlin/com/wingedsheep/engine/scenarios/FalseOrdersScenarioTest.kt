package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FalseOrdersScenarioTest : FunSpec({
    val evasive = card("Orders Evasive Attacker") {
        typeLine = "Creature — Bird"; power = 1; toughness = 5
        keywords(Keyword.FLYING, Keyword.MENACE)
    }
    val triggerBlocker = card("Orders Trigger Blocker") {
        typeLine = "Creature — Soldier"; power = 1; toughness = 5
        triggeredAbility { trigger = Triggers.self.blocks(); effect = Effects.GainLife(1) }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(evasive, triggerBlocker))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        putPermanentOnBattlefield(activePlayer!!, "Mountain")
    }
    fun attacker(d: GameTestDriver, name: String = "Grizzly Bears") =
        d.putCreatureOnBattlefield(d.activePlayer!!, name).also(d::removeSummoningSickness)
    fun combat(d: GameTestDriver, attackers: List<EntityId>, blocks: Map<EntityId, List<EntityId>>) {
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, attackers, d.getOpponent(me)).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.getOpponent(me), blocks).error shouldBe null
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
    }
    fun resolveToChoice(d: GameTestDriver, blocker: EntityId): SelectCardsDecision {
        val caster = d.activePlayer!!
        val spell = d.putCardInHand(caster, "False Orders")
        d.castSpell(caster, spell, listOf(blocker)).error shouldBe null
        d.bothPass().error shouldBe null
        val choice = d.pendingDecision as SelectCardsDecision
        choice.playerId shouldBe caster
        choice.minSelections shouldBe 0
        choice.maxSelections shouldBe 1
        return choice
    }
    fun choose(d: GameTestDriver, cards: List<EntityId>) {
        val choice = d.pendingDecision as SelectCardsDecision
        d.submitCardSelection(choice.playerId, cards).error shouldBe null
        d.pendingDecision shouldBe null
    }
    test("reassigns a sole blocker and makes the original attacker unblocked") {
        val d = driver(); val a = attacker(d); val b = attacker(d)
        val blocker = d.putCreatureOnBattlefield(d.getOpponent(d.activePlayer!!), "Grizzly Bears")
        combat(d, listOf(a, b), mapOf(blocker to listOf(a)))
        val choice = resolveToChoice(d, blocker)
        choice.options.toSet() shouldBe setOf(a, b)
        d.state.getEntity(blocker)!!.has<BlockingComponent>() shouldBe false
        d.state.getEntity(a)!!.has<BlockedComponent>() shouldBe false
        choose(d, listOf(b))
        d.state.getEntity(blocker)!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe listOf(b)
        d.state.getEntity(b)!!.get<BlockedComponent>()!!.blockerIds shouldBe listOf(blocker)
    }
    test("declining keeps the former blocker out of combat") {
        val d = driver(); val a = attacker(d)
        val blocker = d.putCreatureOnBattlefield(d.getOpponent(d.activePlayer!!), "Grizzly Bears")
        combat(d, listOf(a), mapOf(blocker to listOf(a)))
        resolveToChoice(d, blocker); choose(d, emptyList())
        d.state.getEntity(blocker)!!.has<BlockingComponent>() shouldBe false
        d.state.getEntity(a)!!.has<BlockedComponent>() shouldBe false
    }
    test("a tapped creature not previously blocking can block flying and menace") {
        val d = driver(); val a = attacker(d, evasive.name)
        val blocker = d.putCreatureOnBattlefield(d.getOpponent(d.activePlayer!!), "Grizzly Bears")
        d.addComponent(blocker, TappedComponent)
        combat(d, listOf(a), emptyMap())
        resolveToChoice(d, blocker); choose(d, listOf(a))
        d.state.getEntity(blocker)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(blocker)!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe listOf(a)
    }
    test("choosing an attacker does not target it") {
        val shroud = card("Orders Shroud Attacker") {
            typeLine = "Creature — Beast"; power = 1; toughness = 5; keywords(Keyword.SHROUD)
        }
        val d = driver(); d.registerCards(listOf(shroud)); val a = attacker(d, shroud.name)
        val blocker = d.putCreatureOnBattlefield(d.getOpponent(d.activePlayer!!), "Grizzly Bears")
        combat(d, listOf(a), emptyMap())
        resolveToChoice(d, blocker).options shouldBe listOf(a)
        choose(d, listOf(a))
        d.state.getEntity(a)!!.has<BlockedComponent>() shouldBe true
    }
    test("an attacker with another current blocker remains blocked") {
        val d = driver(); val a = attacker(d); val opp = d.getOpponent(d.activePlayer!!)
        val first = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        combat(d, listOf(a), mapOf(first to listOf(a), second to listOf(a)))
        resolveToChoice(d, first); choose(d, emptyList())
        d.state.getEntity(a)!!.get<BlockedComponent>()!!.blockerIds shouldBe listOf(second)
    }
    test("a departed second blocker still prevents becoming unblocked") {
        val d = driver(); val a = attacker(d); val opp = d.getOpponent(d.activePlayer!!)
        val first = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        combat(d, listOf(a), mapOf(first to listOf(a), second to listOf(a)))
        val destruction = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Destroy(com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(0)),
            com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = d.activePlayer!!,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(second))))
        destruction.error shouldBe null
        d.replaceState(destruction.state)
        resolveToChoice(d, first); choose(d, emptyList())
        d.state.getEntity(a)!!.has<BlockedComponent>() shouldBe true
        d.state.getEntity(a)!!.get<BlockedComponent>()!!.blockerIds shouldBe emptyList()
    }
    test("an illegal spell target fizzles without asking for an attacker") {
        val d = driver(); val a = attacker(d); val opp = d.getOpponent(d.activePlayer!!)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        combat(d, listOf(a), mapOf(blocker to listOf(a)))
        val spell = d.putCardInHand(d.activePlayer!!, "False Orders")
        d.castSpell(d.activePlayer!!, spell, listOf(blocker)).error shouldBe null
        d.moveToGraveyard(blocker)
        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
        d.state.stack shouldBe emptyList()
        d.state.getEntity(a)!!.has<BlockedComponent>() shouldBe true
    }
    test("a new block triggers when the choice resumes") {
        val d = driver(); val a = attacker(d); val opp = d.getOpponent(d.activePlayer!!)
        val blocker = d.putCreatureOnBattlefield(opp, triggerBlocker.name)
        combat(d, listOf(a), emptyMap())
        resolveToChoice(d, blocker)
        d.getLifeTotal(opp) shouldBe 20
        choose(d, listOf(a))
        d.state.stack.size shouldBe 1
        d.bothPass().error shouldBe null
        d.getLifeTotal(opp) shouldBe 21
    }
    test("removing a declared blocker preserves its already stacked block trigger") {
        val d = driver(); val a = attacker(d); val opp = d.getOpponent(d.activePlayer!!)
        val blocker = d.putCreatureOnBattlefield(opp, triggerBlocker.name)
        combat(d, listOf(a), mapOf(blocker to listOf(a)))
        d.state.stack.size shouldBe 1
        resolveToChoice(d, blocker); choose(d, emptyList())
        d.state.stack.size shouldBe 1
        d.bothPass().error shouldBe null
        d.getLifeTotal(opp) shouldBe 21
    }
    test("removing and reblocking the same attacker triggers blocks again") {
        val d = driver(); val a = attacker(d); val opp = d.getOpponent(d.activePlayer!!)
        val blocker = d.putCreatureOnBattlefield(opp, triggerBlocker.name)
        combat(d, listOf(a), mapOf(blocker to listOf(a)))
        d.state.stack.size shouldBe 1
        resolveToChoice(d, blocker); choose(d, listOf(a))
        d.state.stack.size shouldBe 2
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        d.getLifeTotal(opp) shouldBe 22
    }
    test("casting is restricted to declare blockers and defending creatures") {
        val d = driver(); val a = attacker(d); val opp = d.getOpponent(d.activePlayer!!)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val spell = d.putCardInHand(d.activePlayer!!, "False Orders")
        d.castSpell(d.activePlayer!!, spell, listOf(blocker)).error shouldNotBe null
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.activePlayer!!, listOf(a), opp).error shouldBe null
        d.castSpell(d.activePlayer!!, spell, listOf(blocker)).error shouldNotBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, emptyMap()).error shouldBe null
        d.castSpell(d.activePlayer!!, spell, listOf(a)).error shouldNotBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.castSpell(d.activePlayer!!, spell, listOf(blocker)).error shouldNotBe null
    }
})
