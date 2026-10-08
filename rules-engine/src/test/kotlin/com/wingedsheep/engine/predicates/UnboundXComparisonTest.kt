package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CompareNumericProperty` over `DynamicAmount.XValue` while X is unbound.
 *
 * Legal-action enumeration runs before the player picks X, so there it must match permissively or
 * "target creature with power X" is never offered. Inside a resolution an unbound X is a lost value:
 * matching everything there would turn "destroy all creatures with power X or greater" into a board
 * wipe, so it matches nothing.
 */
class UnboundXComparisonTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val powerAtLeastX = GameObjectFilter.Creature.powerAtLeastX()

    fun GameTestDriver.matches(entity: EntityId, context: PredicateContext) =
        services.predicateEvaluator.matches(state, state.projectedState, entity, powerAtLeastX, context)

    test("enumeration — no resolution, X unbound — matches permissively") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.matches(bears, PredicateContext(controllerId = me)) shouldBe true
    }

    test("a resolution with X unbound matches nothing") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val resolving = PredicateContext.fromEffectContext(EffectContext(sourceId = null, controllerId = me))
        withClue("a lost X must not make every creature qualify") {
            d.matches(bears, resolving) shouldBe false
        }
    }

    test("a bound X compares, in a resolution or not") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val resolving = { x: Int -> PredicateContext.fromEffectContext(EffectContext(sourceId = null, controllerId = me, xValue = x)) }
        d.matches(bears, resolving(2)) shouldBe true
        d.matches(bears, resolving(3)) shouldBe false
        d.matches(bears, PredicateContext(controllerId = me, xValue = 3)) shouldBe false
    }
})
