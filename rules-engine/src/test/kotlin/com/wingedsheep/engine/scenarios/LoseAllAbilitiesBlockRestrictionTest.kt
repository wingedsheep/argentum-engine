package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Can't block" and "can't be blocked" under a lose-all-abilities effect.
 *
 * A static ability's effect exists only while the object has the ability (CR 604.2), and a
 * restriction such as "can't block" is a rules effect applied after characteristics are settled
 * (CR 613.11). So a creature's *own* "This creature can't block" (Craven Giant) stops applying
 * once the creature loses all abilities — whatever the timestamps — exactly as its own
 * "can't be blocked" does. A "can't block" imposed by *another* source (an enchantment's static,
 * a resolved spell's continuous effect) is not an ability of the creature and stays in force; it
 * ends only when that source loses the ability (static) or its duration runs out (CR 611.2a).
 */
class LoseAllAbilitiesBlockRestrictionTest : FunSpec({
    val craven = card("Craven Test Giant") {
        manaCost = "{2}{R}"; typeLine = "Creature — Giant"; power = 4; toughness = 1
        staticAbility { ability = CantBlock() }
    }
    val sneak = card("Unblockable Test Sneak") {
        manaCost = "{1}{U}"; typeLine = "Creature — Rogue"; power = 1; toughness = 1
        staticAbility { ability = CantBeBlocked() }
    }
    val cowardice = card("Mass Cowardice Test Field") {
        manaCost = "{2}{R}"; typeLine = "Enchantment"
        staticAbility { ability = CantBlock(GroupFilter.AllCreatures) }
    }
    val humility = card("Humility Test Field") {
        manaCost = "{2}{W}{W}"; typeLine = "Enchantment"
        staticAbility { ability = LoseAllAbilities(GroupFilter.AllCreatures) }
    }
    val holder = card("Restriction Test Holder") { typeLine = "Enchantment" }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(craven, sneak, cowardice, humility, holder))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.run(effect: Effect, source: EntityId, controller: EntityId) {
        val result = services.effectExecutorRegistry.execute(state, effect,
            EffectContext(sourceId = source, controllerId = controller))
        result.error shouldBe null
        replaceState(result.state)
    }

    /** Attack with a fresh bear and try to block it with [blocker]; returns the block error. */
    fun GameTestDriver.tryBlock(attackerOwner: EntityId, blocker: EntityId): String? {
        val defender = getOpponent(attackerOwner)
        val bear = putCreatureOnBattlefield(attackerOwner, "Grizzly Bears")
        removeSummoningSickness(bear)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attackerOwner, listOf(bear), defender).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        return declareBlockers(defender, mapOf(blocker to listOf(bear))).error
    }

    test("a creature's own can't-block ends when it loses all abilities") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val giant = d.putCreatureOnBattlefield(opp, craven.name)
        d.state.projectedState.cantBlock(giant) shouldBe true
        d.run(Effects.RemoveAllAbilities(EffectTarget.Self), giant, opp)
        d.state.projectedState.cantBlock(giant) shouldBe false
        d.tryBlock(me, giant) shouldBe null
    }

    test("an earlier lose-all-abilities static still strips a later creature's own can't-block") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, humility.name)
        val giant = d.putCreatureOnBattlefield(opp, craven.name)
        d.state.projectedState.cantBlock(giant) shouldBe false
        d.tryBlock(me, giant) shouldBe null
    }

    test("a creature's own can't-be-blocked ends the same way, in either timestamp order") {
        val d = driver(); val me = d.activePlayer!!
        val early = d.putCreatureOnBattlefield(me, sneak.name)
        d.state.projectedState.hasKeyword(early, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        d.run(Effects.RemoveAllAbilities(EffectTarget.Self), early, me)
        d.state.projectedState.hasKeyword(early, AbilityFlag.CANT_BE_BLOCKED) shouldBe false

        d.putPermanentOnBattlefield(me, humility.name)
        val late = d.putCreatureOnBattlefield(me, sneak.name)
        d.state.projectedState.hasKeyword(late, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
    }

    test("a resolved spell's can't-block survives the creature losing all abilities") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(me, holder.name)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.CantBlock(EffectTarget.SpecificEntity(blocker)),
            EffectContext(sourceId = source, controllerId = me))
        result.error shouldBe null; d.replaceState(result.state)
        d.run(Effects.RemoveAllAbilities(EffectTarget.Self), blocker, opp)
        d.state.projectedState.cantBlock(blocker) shouldBe true
        d.tryBlock(me, blocker) shouldNotBe null
    }

    test("another permanent's can't-block static survives the creature losing all abilities") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, cowardice.name)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.run(Effects.RemoveAllAbilities(EffectTarget.Self), blocker, opp)
        d.state.projectedState.cantBlock(blocker) shouldBe true
        d.tryBlock(me, blocker) shouldNotBe null
    }

    test("another permanent's can't-block static ends when that permanent loses all abilities") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val field = d.putPermanentOnBattlefield(me, cowardice.name)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.state.projectedState.cantBlock(blocker) shouldBe true
        d.run(Effects.RemoveAllAbilities(EffectTarget.Self), field, me)
        d.state.projectedState.cantBlock(blocker) shouldBe false
        d.tryBlock(me, blocker) shouldBe null
    }
})
