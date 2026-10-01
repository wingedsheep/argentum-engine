package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.StaticAbilityGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.combat.rules.AttackCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.CantBeAttackedByDefenderRule
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FloatingDefenderRestrictionsTest : FunSpec({
    val holder = card("Restriction Test Holder") { typeLine = "Enchantment" }
    val flier = card("Restriction Test Flier") {
        typeLine = "Creature — Bird"; power = 2; toughness = 2; keywords(Keyword.FLYING)
    }
    val walker = card("Restriction Test Walker") {
        typeLine = "Planeswalker — Tester"; startingLoyalty = 3
    }
    val groundRestriction = CantBeAttackedBy(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))
    val printedHolder = card("Restriction Test Printed Holder") {
        typeLine = "Enchantment"
        staticAbility { ability = groundRestriction }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(holder, printedHolder, flier, walker))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.grant(to: EntityId, source: EntityId, ability: StaticAbility = groundRestriction,
                             duration: Duration = Duration.EndOfTurn) {
        val result = services.effectExecutorRegistry.execute(state,
            Effects.GrantStaticAbility(ability, EffectTarget.Controller, duration),
            EffectContext(sourceId = source, controllerId = to))
        result.error shouldBe null
        result.events shouldBe listOf(StaticAbilityGrantedEvent(to))
        replaceState(result.state)
    }
    fun GameTestDriver.restriction(attacker: EntityId, defender: EntityId): String? =
        CantBeAttackedByDefenderRule(services.predicateEvaluator).check(
            AttackCheckContext(state, state.projectedState, attacker, activePlayer!!, cardRegistry), defender)

    test("player grant evaluates late entrants and bypasses planeswalkers") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, holder.name)
        d.grant(opp, source)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val bird = d.putCreatureOnBattlefield(me, flier.name)
        val pw = d.putPermanentOnBattlefield(opp, walker.name)
        d.restriction(bear, opp) shouldNotBe null
        d.restriction(bird, opp) shouldBe null
        d.restriction(bear, pw) shouldBe null
        d.restriction(bear, me) shouldBe null
    }
    test("projected keyword gains and removals change attack eligibility") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, holder.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.grant(opp, source)
        val gain = d.services.effectExecutorRegistry.execute(d.state,
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
            EffectContext(sourceId = bear, controllerId = me))
        gain.error shouldBe null; d.replaceState(gain.state)
        d.restriction(bear, opp) shouldBe null
        val lose = d.services.effectExecutorRegistry.execute(d.state,
            Effects.RemoveAllAbilities(EffectTarget.Self), EffectContext(sourceId = bear, controllerId = me))
        lose.error shouldBe null; d.replaceState(lose.state)
        d.restriction(bear, opp) shouldNotBe null
    }
    test("source departure preserves a fixed-duration player grant") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, holder.name)
        d.grant(opp, source, duration = Duration.UntilYourNextTurn)
        val destroyed = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Destroy(EffectTarget.Self), EffectContext(sourceId = source, controllerId = opp))
        destroyed.error shouldBe null; d.replaceState(destroyed.state)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.restriction(bear, opp) shouldNotBe null
    }
    test("source-keyed duration closes before a settle pass") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, holder.name)
        d.grant(opp, source, duration = Duration.WhileSourceOnBattlefield())
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.restriction(bear, opp) shouldNotBe null
        d.moveToGraveyard(source)
        d.restriction(bear, opp) shouldBe null
    }
    test("conditional and composite grants compose their restrictions") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, holder.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.grant(opp, source, CompositeStaticAbility(listOf(
            ConditionalStaticAbility(groundRestriction, Conditions.IsYourTurn))))
        d.restriction(bear, opp) shouldBe null
        d.replaceState(d.state.copy(activePlayerId = opp))
        d.restriction(bear, opp) shouldNotBe null
    }
    test("stacked grants all apply and a flying-only restriction uses the opposite polarity") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, holder.name)
        d.grant(opp, source)
        d.grant(opp, source, CantBeAttackedBy(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)))
        val bird = d.putCreatureOnBattlefield(me, flier.name)
        d.restriction(bird, opp) shouldNotBe null
        d.services.combatManager.isRestrictedFromAllDefenders(d.state, bird, me) shouldBe true
    }
    test("until-next-turn grant survives cleanup and expires on its granting controller's turn") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(me, holder.name)
        d.grant(me, source, duration = Duration.UntilYourNextTurn)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opp
        d.state.grantedStaticAbilities.size shouldBe 1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe me
        d.state.grantedStaticAbilities shouldBe emptyList()
    }
    test("printed restrictions stop contributing after ability loss") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, printedHolder.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.restriction(bear, opp) shouldNotBe null
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.RemoveAllAbilities(EffectTarget.Self), EffectContext(sourceId = source, controllerId = opp))
        result.error shouldBe null; d.replaceState(result.state)
        d.restriction(bear, opp) shouldBe null
    }
    test("permanent-held runtime restrictions contribute while their holder remains in play") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, holder.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.GrantStaticAbility(groundRestriction, EffectTarget.Self),
            EffectContext(sourceId = source, controllerId = opp))
        result.error shouldBe null; d.replaceState(result.state)
        d.restriction(bear, opp) shouldNotBe null
        d.moveToGraveyard(source)
        d.restriction(bear, opp) shouldBe null
    }
    test("end-of-turn grants expire at cleanup") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opp, holder.name)
        d.grant(opp, source)
        d.passPriorityUntil(Step.UPKEEP)
        d.state.grantedStaticAbilities shouldBe emptyList()
    }

})
