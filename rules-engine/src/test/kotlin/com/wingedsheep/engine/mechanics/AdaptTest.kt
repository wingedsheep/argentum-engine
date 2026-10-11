package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Adapt (CR 701.46a — "If this permanent has no +1/+1 counters on it, put N +1/+1 counters on
 * it") and the one-shot "the next time target creature adapts this turn, it adapts as though it
 * had no +1/+1 counters on it" shield (Biomancer's Familiar).
 *
 * The rulings pinned here: adapt checks counters on resolution (the ability can always be
 * activated); a creature that loses its counters can adapt again; the shield adds no counters
 * itself, is used up by the creature's next adapt whether or not that adapt needed it, lasts only
 * this turn, and belongs to the one creature it targeted.
 */
class AdaptTest : FunSpec({

    val adapter = card("Test Adapter") {
        manaCost = "{0}"
        typeLine = "Creature — Mutant"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Adapt(2)
        }
    }

    val familiar = card("Test Adapt Familiar") {
        manaCost = "{0}"
        typeLine = "Creature — Mutant"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Free
            val creature = target(TargetFilter.Creature)
            effect = Effects.AdaptAsThoughNoCounters(creature)
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + adapter + familiar)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.plusOnes(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.setPlusOnes(entity: EntityId, n: Int) = replaceState(
        state.updateEntity(entity) { c ->
            val current = c.get<CountersComponent>() ?: CountersComponent()
            c.with(current.withAdded(CounterType.PLUS_ONE_PLUS_ONE, n - current.getCount(CounterType.PLUS_ONE_PLUS_ONE)))
        }
    )

    fun GameTestDriver.adapt(player: EntityId, creature: EntityId) {
        submitSuccess(ActivateAbility(player, creature, adapter.activatedAbilities[0].id))
        bothPass()
    }

    fun GameTestDriver.shield(player: EntityId, source: EntityId, target: EntityId) {
        submitSuccess(
            ActivateAbility(player, source, familiar.activatedAbilities[0].id, targets = listOf(ChosenTarget.Permanent(target)))
        )
        bothPass()
    }

    fun GameTestDriver.shieldCount(entity: EntityId): Int = state.floatingEffects.count {
        it.effect.modification is SerializableModification.AdaptAsThoughNoCountersShield &&
            entity in it.effect.affectedEntities
    }

    test("adapt puts N +1/+1 counters on a creature with none, and nothing once it has some") {
        val d = driver()
        val me = d.activePlayer!!
        val creature = d.putCreatureOnBattlefield(me, "Test Adapter")
        d.adapt(me, creature)
        d.plusOnes(creature) shouldBe 2
        d.adapt(me, creature)
        d.plusOnes(creature) shouldBe 2
    }

    test("a creature that lost all its +1/+1 counters can adapt again") {
        val d = driver()
        val me = d.activePlayer!!
        val creature = d.putCreatureOnBattlefield(me, "Test Adapter")
        d.adapt(me, creature)
        d.setPlusOnes(creature, 0)
        d.adapt(me, creature)
        d.plusOnes(creature) shouldBe 2
    }

    test("the shield adds no counters; the next adapt ignores existing counters and uses it up") {
        val d = driver()
        val me = d.activePlayer!!
        val creature = d.putCreatureOnBattlefield(me, "Test Adapter")
        val source = d.putCreatureOnBattlefield(me, "Test Adapt Familiar")
        d.setPlusOnes(creature, 1)

        d.shield(me, source, creature)
        d.plusOnes(creature) shouldBe 1
        d.shieldCount(creature) shouldBe 1

        d.adapt(me, creature)
        d.plusOnes(creature) shouldBe 3
        d.shieldCount(creature) shouldBe 0

        // One shield, one adapt: the next adapt sees the counters again.
        d.adapt(me, creature)
        d.plusOnes(creature) shouldBe 3
    }

    test("an adapt that didn't need the shield still uses it up") {
        val d = driver()
        val me = d.activePlayer!!
        val creature = d.putCreatureOnBattlefield(me, "Test Adapter")
        val source = d.putCreatureOnBattlefield(me, "Test Adapt Familiar")

        d.shield(me, source, creature)
        d.adapt(me, creature)
        d.plusOnes(creature) shouldBe 2
        d.shieldCount(creature) shouldBe 0

        d.adapt(me, creature)
        d.plusOnes(creature) shouldBe 2
    }

    test("two shields let two adapts ignore counters") {
        val d = driver()
        val me = d.activePlayer!!
        val creature = d.putCreatureOnBattlefield(me, "Test Adapter")
        val source = d.putCreatureOnBattlefield(me, "Test Adapt Familiar")
        d.setPlusOnes(creature, 1)

        d.shield(me, source, creature)
        d.shield(me, source, creature)
        d.adapt(me, creature)
        d.adapt(me, creature)
        d.plusOnes(creature) shouldBe 5
        d.shieldCount(creature) shouldBe 0
    }

    test("the shield belongs to the creature it targeted") {
        val d = driver()
        val me = d.activePlayer!!
        val shielded = d.putCreatureOnBattlefield(me, "Test Adapter")
        val other = d.putCreatureOnBattlefield(me, "Test Adapter")
        val source = d.putCreatureOnBattlefield(me, "Test Adapt Familiar")
        d.setPlusOnes(shielded, 1)
        d.setPlusOnes(other, 1)

        d.shield(me, source, shielded)
        d.adapt(me, other)
        d.plusOnes(other) shouldBe 1
        d.shieldCount(shielded) shouldBe 1
    }

    test("the shield lasts only this turn") {
        val d = driver()
        val me = d.activePlayer!!
        val creature = d.putCreatureOnBattlefield(me, "Test Adapter")
        val source = d.putCreatureOnBattlefield(me, "Test Adapt Familiar")
        d.setPlusOnes(creature, 1)

        d.shield(me, source, creature)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(me))
        d.shieldCount(creature) shouldBe 0
    }
})
