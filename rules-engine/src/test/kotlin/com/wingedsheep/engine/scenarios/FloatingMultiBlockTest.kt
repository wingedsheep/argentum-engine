package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.StaticAbilityGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.combat.BlockStaticRules
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FloatingMultiBlockTest : FunSpec({
    val holder = card("Multi Block Holder") { typeLine = "Enchantment" }
    val wall = card("Multi Block Wall") { typeLine = "Creature"; power = 0; toughness = 10 }
    val flier = card("Multi Block Flier") { typeLine = "Creature"; power = 1; toughness = 1; keywords(Keyword.FLYING) }
    val menace = card("Multi Block Menace") { typeLine = "Creature"; power = 1; toughness = 1; keywords(Keyword.MENACE) }
    val printed = card("Multi Block Printed") {
        typeLine = "Creature"; power = 0; toughness = 10
        staticAbility { ability = CanBlockAnyNumber() }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(holder, wall, flier, menace, printed))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.rules() = BlockStaticRules(state, cardRegistry, services.predicateEvaluator)
    fun GameTestDriver.grant(to: EntityId, ability: StaticAbility, duration: Duration = Duration.EndOfTurn,
                             source: EntityId = to) {
        val result = services.effectExecutorRegistry.execute(state,
            Effects.GrantStaticAbility(ability, EffectTarget.ContextTarget(0), duration),
            EffectContext(sourceId = source, controllerId = activePlayer!!, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(to))))
        result.error shouldBe null; result.events shouldBe listOf(StaticAbilityGrantedEvent(to))
        replaceState(result.state)
    }
    fun GameTestDriver.blocks(attackers: List<EntityId>, defender: EntityId) {
        var next = state.copy(step = Step.DECLARE_BLOCKERS, priorityPlayerId = defender)
        for (attacker in attackers) next = next.updateEntity(attacker) { it.with(AttackingComponent(defender)) }
        replaceState(next)
    }
    test("floating unlimited capacity agrees with offered legal action and declaration") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val a = d.putCreatureOnBattlefield(me, "Grizzly Bears"); val b = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val w = d.putCreatureOnBattlefield(opp, wall.name)
        d.grant(w, CanBlockAnyNumber()); d.blocks(listOf(a,b),opp)
        d.legalActions(opp).single().blockerMaxBlockCounts!![w] shouldBe Int.MAX_VALUE
        d.declareBlockers(opp, mapOf(w to listOf(a,b))).error shouldBe null
    }
    test("each-attacker requirement rejects partial declaration and suggests the complete block") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val a = d.putCreatureOnBattlefield(me,"Grizzly Bears"); val b = d.putCreatureOnBattlefield(me,"Grizzly Bears")
        val w = d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,CanBlockAnyNumber()); d.grant(w,MustBlockEachAttacker()); d.blocks(listOf(a,b),opp)
        d.legalActions(opp).single().mandatoryBlockerAssignments shouldBe mapOf(w to listOf(a,b))
        d.declareBlockers(opp,mapOf(w to listOf(a))).error shouldNotBe null
        d.declareBlockers(opp,mapOf(w to listOf(a,b))).error shouldBe null
    }
    test("separate requirements permit choosing one attacker when capacity is one") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val a = d.putCreatureOnBattlefield(me,"Grizzly Bears"); val b = d.putCreatureOnBattlefield(me,"Grizzly Bears")
        val w = d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,MustBlockEachAttacker()); d.blocks(listOf(a,b),opp)
        d.declareBlockers(opp,emptyMap()).error shouldNotBe null
        d.declareBlockers(opp,mapOf(w to listOf(b))).error shouldBe null
    }
    test("flying remains un-blockable and does not create an impossible requirement") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val a = d.putCreatureOnBattlefield(me,"Grizzly Bears"); val b = d.putCreatureOnBattlefield(me,flier.name)
        val w = d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,CanBlockAnyNumber()); d.grant(w,MustBlockEachAttacker()); d.blocks(listOf(a,b),opp)
        d.declareBlockers(opp,mapOf(w to listOf(a))).error shouldBe null
    }
    test("a lone blocker cannot be compelled to block menace") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val a = d.putCreatureOnBattlefield(me,menace.name); val w = d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,MustBlockEachAttacker()); d.blocks(listOf(a),opp)
        d.declareBlockers(opp,emptyMap()).error shouldBe null
    }
    test("large menace combat removes impossible requirements before assignment search") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val attackers = List(24) { d.putCreatureOnBattlefield(me, menace.name) }
        val w = d.putCreatureOnBattlefield(opp, wall.name)
        d.grant(w, CanBlockAnyNumber()); d.grant(w, MustBlockEachAttacker())
        d.blocks(attackers, opp)
        d.legalActions(opp).single().mandatoryBlockerAssignments.orEmpty() shouldBe emptyMap()
        d.declareBlockers(opp, emptyMap()).error shouldBe null
    }
    test("large menace combat respects limited companion capacity during assignment search") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val attackers = List(24) { d.putCreatureOnBattlefield(me, menace.name) }
        val w = d.putCreatureOnBattlefield(opp, wall.name)
        val companion = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.grant(w, CanBlockAnyNumber()); d.grant(w, MustBlockEachAttacker())
        d.blocks(attackers, opp)
        val suggestion = d.legalActions(opp).single().mandatoryBlockerAssignments!!
        suggestion.keys shouldBe setOf(w, companion)
        suggestion.getValue(w).size shouldBe 1
        suggestion.getValue(companion) shouldBe suggestion.getValue(w)
        d.declareBlockers(opp, emptyMap()).error shouldNotBe null
        d.declareBlockers(opp, suggestion).error shouldBe null
    }
    test("menace companion is included in a maximal legal suggested declaration") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val a = d.putCreatureOnBattlefield(me,menace.name); val w = d.putCreatureOnBattlefield(opp,wall.name)
        val companion = d.putCreatureOnBattlefield(opp,"Grizzly Bears")
        d.grant(w,MustBlockEachAttacker()); d.blocks(listOf(a),opp)
        val suggestion = d.legalActions(opp).single().mandatoryBlockerAssignments!!
        suggestion shouldBe mapOf(w to listOf(a), companion to listOf(a))
        d.declareBlockers(opp,emptyMap()).error shouldNotBe null
        d.declareBlockers(opp,suggestion).error shouldBe null
    }
    test("tapped target gains capacity but remains unable to block") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val a = d.putCreatureOnBattlefield(me,"Grizzly Bears"); val w = d.putCreatureOnBattlefield(opp,wall.name)
        d.addComponent(w,TappedComponent); d.grant(w,CanBlockAnyNumber()); d.grant(w,MustBlockEachAttacker())
        d.blocks(listOf(a),opp); d.declareBlockers(opp,emptyMap()).error shouldBe null
    }
    test("group scope uses projected blocker characteristics and nested conditions") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp,holder.name); val w = d.putCreatureOnBattlefield(opp,wall.name)
        val group = CanBlockAnyNumber(GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)))
        d.grant(source,CompositeStaticAbility(listOf(ConditionalStaticAbility(group,Conditions.IsYourTurn))))
        d.rules().maxBlocks(w) shouldBe 1
        d.replaceState(d.state.copy(activePlayerId = opp))
        val result = d.services.effectExecutorRegistry.execute(d.state, Effects.GrantKeyword(Keyword.FLYING,EffectTarget.Self),
            EffectContext(sourceId = w,controllerId = opp))
        d.replaceState(result.state); d.rules().maxBlocks(w) shouldBe Int.MAX_VALUE
    }
    test("source-dependent grant closes before settle and fixed-duration grants survive source departure") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(me,holder.name); val w = d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,CanBlockAnyNumber(),Duration.WhileSourceOnBattlefield(),source)
        d.rules().maxBlocks(w) shouldBe Int.MAX_VALUE; d.moveToGraveyard(source); d.rules().maxBlocks(w) shouldBe 1
        d.grant(w,CanBlockAnyNumber(),source=source); d.rules().maxBlocks(w) shouldBe Int.MAX_VALUE
    }
    test("cleanup removes floating capacity and requirements") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val w = d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,CanBlockAnyNumber()); d.grant(w,MustBlockEachAttacker())
        d.passPriorityUntil(Step.UPKEEP); d.rules().maxBlocks(w) shouldBe 1; d.rules().mustBlockEach(w) shouldBe false
    }
    test("printed unlimited block ability disappears when the creature loses abilities") {
        val d = driver(); val me = d.activePlayer!!; val w = d.putCreatureOnBattlefield(me,printed.name)
        d.rules().maxBlocks(w) shouldBe Int.MAX_VALUE
        val result = d.services.effectExecutorRegistry.execute(d.state,Effects.RemoveAllAbilities(EffectTarget.Self),
            EffectContext(sourceId=w,controllerId=me)); d.replaceState(result.state)
        d.rules().maxBlocks(w) shouldBe 1
    }
    test("blocking cost is voluntary despite a requirement") {
        val taxHolder = card("Multi Block Tax") {
            typeLine = "Enchantment"
            staticAbility { ability = BlockTax(DynamicAmounts.fixed(1)) }
        }
        val d = driver(); d.registerCards(listOf(taxHolder))
        val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me,taxHolder.name)
        val a = d.putCreatureOnBattlefield(me,"Grizzly Bears"); val w = d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,MustBlockEachAttacker()); d.blocks(listOf(a),opp)
        d.declareBlockers(opp,emptyMap()).error shouldBe null
    }
    test("global blocker cap permits either of equally maximal required blockers") {
        val cap = card("Multi Block Cap") {
            typeLine = "Enchantment"; staticAbility { ability = BlockerCountLimit(1) }
        }
        val d = driver(); d.registerCards(listOf(cap)); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me,cap.name)
        val a = d.putCreatureOnBattlefield(me,"Grizzly Bears")
        val w = d.putCreatureOnBattlefield(opp,wall.name); val other = d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,MustBlockEachAttacker()); d.grant(other,MustBlockEachAttacker()); d.blocks(listOf(a),opp)
        d.declareBlockers(opp,emptyMap()).error shouldNotBe null
        d.declareBlockers(opp,mapOf(other to listOf(a))).error shouldBe null
    }
    test("resolved combat rules survive removing the target's abilities") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val w=d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,CanBlockAnyNumber());d.grant(w,MustBlockEachAttacker())
        val result=d.services.effectExecutorRegistry.execute(d.state,Effects.RemoveAllAbilities(EffectTarget.Self),
            EffectContext(sourceId=w,controllerId=opp));d.replaceState(result.state)
        d.rules().maxBlocks(w) shouldBe Int.MAX_VALUE;d.rules().mustBlockEach(w) shouldBe true
    }
    test("defending-controller predicate covers all multiplayer defenders before attacks and honors attack mode") {
        val d=GameTestDriver();d.registerCards(TestCards.all+listOf(wall))
        val seats=d.initMultiplayer(List(3) { Deck.of("Forest" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val own=d.putCreatureOnBattlefield(seats[0],wall.name)
        val left=d.putCreatureOnBattlefield(seats[1],wall.name)
        val right=d.putCreatureOnBattlefield(seats[2],wall.name)
        val filter=GameObjectFilter.Creature.defendingPlayerControls()
        fun matches(id:EntityId)=d.services.predicateEvaluator.matches(d.state,d.state.projectedState,id,filter,
            com.wingedsheep.engine.handlers.PredicateContext(sourceId=left,controllerId=seats[1]))
        matches(left) shouldBe false
        d.replaceState(d.state.copy(step=Step.BEGIN_COMBAT))
        matches(own) shouldBe false;matches(left) shouldBe true;matches(right) shouldBe true
        d.replaceState(d.state.copy(attackMode=com.wingedsheep.sdk.core.AttackMode.LEFT))
        matches(left) shouldBe true;matches(right) shouldBe false
    }
    test("Lure and each-attacker requirements are maximized together") {
        val lure=card("Multi Block Lure") {
            typeLine="Creature";power=1;toughness=1
            staticAbility { ability=MustBeBlocked(allCreatures=true) }
        }
        val d=driver();d.registerCards(listOf(lure));val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val a=d.putCreatureOnBattlefield(me,lure.name);val b=d.putCreatureOnBattlefield(me,"Grizzly Bears")
        val w=d.putCreatureOnBattlefield(opp,wall.name)
        d.grant(w,MustBlockEachAttacker());d.grant(w,MustBlockEachAttacker())
        d.rules().eachRequirementCount(w) shouldBe 2
        d.blocks(listOf(a,b),opp)
        d.declareBlockers(opp,mapOf(w to listOf(b))).error shouldNotBe null
        d.declareBlockers(opp,mapOf(w to listOf(a))).error shouldBe null
    }
})
