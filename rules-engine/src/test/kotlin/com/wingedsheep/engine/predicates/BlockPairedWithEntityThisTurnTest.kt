package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.BlockDirection
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.BlockPairedWithEntityThisTurn` — the turn-scoped block record, read in each
 * [BlockDirection] from the candidate's side: "creatures that blocked it" ([BlockDirection.BLOCKED]),
 * "creatures that were blocked by it" ([BlockDirection.BLOCKED_BY], Glyph of Doom), and either way
 * round ([BlockDirection.EITHER], Gaze of the Gorgon).
 */
class BlockPairedWithEntityThisTurnTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attacker(defender: EntityId): EntityId =
        putCreatureOnBattlefield(activePlayer!!, "Grizzly Bears").also { addComponent(it, AttackingComponent(defender)) }

    fun GameTestDriver.block(blocker: EntityId, attacker: EntityId) {
        val result = services.effectExecutorRegistry.execute(state,
            Effects.BecomeBlocking(EffectTarget.ContextTarget(0), EffectTarget.ContextTarget(1)),
            EffectContext(sourceId = null, controllerId = activePlayer!!,
                targets = listOf(ChosenTarget.Permanent(blocker), ChosenTarget.Permanent(attacker))))
        result.error shouldBe null
        replaceState(result.state)
    }

    fun GameTestDriver.removeFromCombat(id: EntityId) {
        val result = services.effectExecutorRegistry.execute(state,
            Effects.RemoveFromCombat(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = null, controllerId = activePlayer!!, targets = listOf(ChosenTarget.Permanent(id))))
        replaceState(result.state)
    }

    /** Does [candidate] match "paired with [reference] in [direction] this turn"? */
    fun GameTestDriver.paired(candidate: EntityId, reference: EntityId?, direction: BlockDirection): Boolean {
        val filter = GameObjectFilter.Creature.withStatePredicate(
            StatePredicate.BlockPairedWithEntityThisTurn(EffectTarget.ContextTarget(0), direction)
        )
        val context = PredicateContext(
            controllerId = activePlayer!!,
            targets = listOfNotNull(reference?.let { ChosenTarget.Permanent(it) })
        )
        return services.predicateEvaluator.matches(state, state.projectedState, candidate, filter, context)
    }

    test("each direction reads its own side of the block") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val attacker = d.attacker(opp)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.block(blocker, attacker)

        // The attacker was blocked by the blocker; it didn't block it.
        d.paired(attacker, blocker, BlockDirection.BLOCKED_BY) shouldBe true
        d.paired(attacker, blocker, BlockDirection.BLOCKED) shouldBe false
        d.paired(attacker, blocker, BlockDirection.EITHER) shouldBe true
        // The blocker blocked the attacker; it wasn't blocked by it.
        d.paired(blocker, attacker, BlockDirection.BLOCKED) shouldBe true
        d.paired(blocker, attacker, BlockDirection.BLOCKED_BY) shouldBe false
        d.paired(blocker, attacker, BlockDirection.EITHER) shouldBe true
    }

    test("a blocker of several attackers was the blocker of each; an unpaired creature matches nothing") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val a = d.attacker(opp); val b = d.attacker(opp); val bystander = d.attacker(opp)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.block(blocker, a); d.block(blocker, b)

        d.paired(a, blocker, BlockDirection.BLOCKED_BY) shouldBe true
        d.paired(b, blocker, BlockDirection.BLOCKED_BY) shouldBe true
        for (direction in BlockDirection.entries) d.paired(bystander, blocker, direction) shouldBe false
    }

    test("the record outlives the pairing: removed from combat, still blocked by it this turn") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val attacker = d.attacker(opp)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.block(blocker, attacker)
        d.removeFromCombat(blocker)

        d.paired(attacker, blocker, BlockDirection.BLOCKED_BY) shouldBe true
    }

    test("a reference that resolves to nothing matches nothing") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val attacker = d.attacker(opp)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.block(blocker, attacker)

        for (direction in BlockDirection.entries) d.paired(attacker, null, direction) shouldBe false
    }
})
