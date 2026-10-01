package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.sdk.model.EntityId

class BlazeOfGloryScenarioTest : FunSpec({
    val wall = card("Glory Test Wall") { typeLine = "Creature — Wall"; power = 0; toughness = 10 }
    val bird = card("Glory Test Bird") { typeLine = "Creature — Bird"; power = 1; toughness = 1; keywords(Keyword.FLYING) }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(wall,bird))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
        putPermanentOnBattlefield(activePlayer!!,"Plains")
    }
    fun cast(d: GameTestDriver, caster: EntityId, target: EntityId) {
        val spell = d.putCardInHand(caster,"Blaze of Glory")
        d.castSpell(caster,spell,listOf(target)).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.stack shouldBe emptyList()
    }
    test("combat trick resolves and requires every blockable attacker") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val a=d.putCreatureOnBattlefield(me,"Grizzly Bears").also(d::removeSummoningSickness)
        val b=d.putCreatureOnBattlefield(me,"Grizzly Bears").also(d::removeSummoningSickness)
        val w=d.putCreatureOnBattlefield(opp,wall.name)
        d.passPriorityUntil(Step.BEGIN_COMBAT);cast(d,me,w)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(a,b),opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        val action=d.legalActions(opp).single()
        action.blockerMaxBlockCounts!![w] shouldBe Int.MAX_VALUE
        action.mandatoryBlockerAssignments shouldBe mapOf(w to listOf(a,b))
        d.declareBlockers(opp,mapOf(w to listOf(a))).error shouldNotBe null
        d.declareBlockers(opp,mapOf(w to listOf(a,b))).error shouldBe null
        d.state.getEntity(w)!!.get<BlockingComponent>()!!.blockedAttackerIds.toSet() shouldBe setOf(a,b)
    }
    test("flying evasion is unchanged") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val a=d.putCreatureOnBattlefield(me,"Grizzly Bears").also(d::removeSummoningSickness)
        val b=d.putCreatureOnBattlefield(me,bird.name).also(d::removeSummoningSickness)
        val w=d.putCreatureOnBattlefield(opp,wall.name)
        d.passPriorityUntil(Step.BEGIN_COMBAT);cast(d,me,w)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(a,b),opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp,mapOf(w to listOf(a))).error shouldBe null
    }
    test("a tapped target cannot block") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val a=d.putCreatureOnBattlefield(me,"Grizzly Bears").also(d::removeSummoningSickness)
        val w=d.putCreatureOnBattlefield(opp,wall.name);d.addComponent(w,TappedComponent)
        d.passPriorityUntil(Step.BEGIN_COMBAT);cast(d,me,w)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(a),opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS);d.declareBlockers(opp,emptyMap()).error shouldBe null
    }
    test("only defending player creatures are legal targets even when attacker casts it") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val own=d.putCreatureOnBattlefield(me,wall.name);val theirs=d.putCreatureOnBattlefield(opp,wall.name)
        val spell=d.putCardInHand(me,"Blaze of Glory")
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.castSpell(me,spell,listOf(own)).error shouldNotBe null
        d.castSpell(me,spell,listOf(theirs)).error shouldBe null
    }
    test("casting is prohibited in main phase and after blockers have been declared") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val a=d.putCreatureOnBattlefield(me,"Grizzly Bears").also(d::removeSummoningSickness)
        val w=d.putCreatureOnBattlefield(opp,wall.name);val spell=d.putCardInHand(me,"Blaze of Glory")
        d.castSpell(me,spell,listOf(w)).error shouldNotBe null
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(a),opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS);d.declareBlockers(opp,emptyMap()).error shouldBe null
        d.castSpell(me,spell,listOf(w)).error shouldNotBe null
    }
    test("casting is permitted after attacks before the declare blockers step") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val a=d.putCreatureOnBattlefield(me,"Grizzly Bears").also(d::removeSummoningSickness)
        val w=d.putCreatureOnBattlefield(opp,wall.name)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(a),opp).error shouldBe null
        cast(d,me,w);d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp,emptyMap()).error shouldNotBe null
        d.declareBlockers(opp,mapOf(w to listOf(a))).error shouldBe null
    }
    test("target departure before resolution leaves no floating grant") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val w=d.putCreatureOnBattlefield(opp,wall.name);val spell=d.putCardInHand(me,"Blaze of Glory")
        d.passPriorityUntil(Step.BEGIN_COMBAT);d.castSpell(me,spell,listOf(w)).error shouldBe null
        d.moveToGraveyard(w);d.bothPass().error shouldBe null
        d.state.grantedStaticAbilities shouldBe emptyList()
    }
    test("defender can cast the spell and its requirement persists into an extra combat") {
        val d=driver();val me=d.activePlayer!!;val opp=d.getOpponent(me)
        val a=d.putCreatureOnBattlefield(me,"Grizzly Bears").also(d::removeSummoningSickness)
        val b=d.putCreatureOnBattlefield(me,"Grizzly Bears").also(d::removeSummoningSickness)
        val w=d.putCreatureOnBattlefield(opp,wall.name)
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.replaceState(d.state.updateEntity(opp) { it.with(com.wingedsheep.engine.state.components.player.ManaPoolComponent(white=1)) })
        d.passPriority(me).error shouldBe null
        cast(d,opp,w)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(a,b),opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS);d.declareBlockers(opp,mapOf(w to listOf(a,b))).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.replaceState(d.state.updateEntity(me) { it.with(com.wingedsheep.engine.state.components.player.ManaPoolComponent(red=2,colorless=2)) })
        val assault=d.putCardInHand(me,"Relentless Assault")
        d.castSpell(me,assault).error shouldBe null;d.bothPass().error shouldBe null
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(a,b),opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp,mapOf(w to listOf(a))).error shouldNotBe null
        d.declareBlockers(opp,mapOf(w to listOf(a,b))).error shouldBe null
    }
})
