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
 * `StatePredicate.BlockPairedWithMatchingThisTurn` — "blocked or was blocked by a <partner filter>
 * this turn" (You Cannot Pass!, Time to Reflect): read from the candidate's turn-scoped block
 * record, with each partner frozen as it stood at the pairing.
 */
class BlockPairedWithMatchingThisTurnTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attacker(defender: EntityId): EntityId =
        putCreatureOnBattlefield(activePlayer!!, "Grizzly Bears").also { addComponent(it, AttackingComponent(defender)) }

    fun GameTestDriver.run(effect: com.wingedsheep.sdk.scripting.effects.Effect, targets: List<EntityId>) {
        val result = services.effectExecutorRegistry.execute(state, effect,
            EffectContext(sourceId = null, controllerId = activePlayer!!,
                targets = targets.map { ChosenTarget.Permanent(it) }))
        result.error shouldBe null
        replaceState(result.state)
    }

    fun GameTestDriver.block(blocker: EntityId, attacker: EntityId) =
        run(Effects.BecomeBlocking(EffectTarget.ContextTarget(0), EffectTarget.ContextTarget(1)), listOf(blocker, attacker))

    fun GameTestDriver.paired(candidate: EntityId, partner: GameObjectFilter, direction: BlockDirection): Boolean {
        val filter = GameObjectFilter.Creature.withStatePredicate(
            StatePredicate.BlockPairedWithMatchingThisTurn(partner, direction)
        )
        return services.predicateEvaluator.matches(
            state, state.projectedState, candidate, filter, PredicateContext(controllerId = activePlayer!!)
        )
    }

    val bear = GameObjectFilter.Creature.withSubtype("Bear")
    val zombie = GameObjectFilter.Creature.withSubtype("Zombie")

    test("reads the partner's characteristics, in each direction") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val attacker = d.attacker(opp)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.block(blocker, attacker)

        d.paired(attacker, bear, BlockDirection.EITHER) shouldBe true
        d.paired(attacker, bear, BlockDirection.BLOCKED_BY) shouldBe true
        d.paired(attacker, bear, BlockDirection.BLOCKED) shouldBe false
        d.paired(blocker, bear, BlockDirection.BLOCKED) shouldBe true
        d.paired(blocker, bear, BlockDirection.BLOCKED_BY) shouldBe false
        // A partner that isn't a Zombie doesn't satisfy "a Zombie".
        d.paired(attacker, zombie, BlockDirection.EITHER) shouldBe false
    }

    test("a creature that fought nobody matches nothing") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val idle = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.paired(idle, bear, BlockDirection.EITHER) shouldBe false
    }

    test("the partner is read as it was at the pairing, after it has left the battlefield") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val attacker = d.attacker(opp)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.block(blocker, attacker)
        d.run(Effects.Destroy(EffectTarget.ContextTarget(0)), listOf(blocker))

        d.state.getBattlefield().contains(blocker) shouldBe false
        d.paired(attacker, bear, BlockDirection.EITHER) shouldBe true
    }

    test("the partner's projected characteristics count: a creature made a Zombie is a Zombie partner") {
        val d = driver(); val opp = d.getOpponent(d.activePlayer!!)
        val attacker = d.attacker(opp)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.run(Effects.AddCreatureType("Zombie", EffectTarget.ContextTarget(0)), listOf(blocker))
        d.block(blocker, attacker)

        d.paired(attacker, zombie, BlockDirection.EITHER) shouldBe true
    }

    test("\"you\" in the partner filter is the asking controller, as the controller stood at the pairing") {
        val d = driver(); val you = d.activePlayer!!; val opp = d.getOpponent(you)
        val attacker = d.attacker(opp)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.block(blocker, attacker)

        d.paired(attacker, GameObjectFilter.Creature.opponentControls(), BlockDirection.EITHER) shouldBe true
        d.paired(attacker, GameObjectFilter.Creature.youControl(), BlockDirection.EITHER) shouldBe false
    }
})
