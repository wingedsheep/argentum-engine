package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.AbilityResolutionCountThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `EntityNumericProperty.AbilityResolutionsThisTurn` — the per-permanent tally behind "the number
 * of times this ability has resolved this turn", and `Conditions.SourceAbilityResolvedNTimes`,
 * which compares it `EQ n`.
 */
class AbilityResolutionsThisTurnTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val predicates = PredicateEvaluator(cardRegistry = null)
    fun GameTestDriver.context(source: EntityId) = EffectContext(sourceId = source, controllerId = player1)
    fun GameTestDriver.resolutions(source: EntityId): Int =
        predicates.amounts.evaluate(state, DynamicAmounts.abilityResolutionsThisTurn(), context(source))
    fun GameTestDriver.bump(source: EntityId, times: Int) = replaceState(
        state.updateEntity(source) { it.with(AbilityResolutionCountThisTurnComponent(times)) }
    )

    test("reads the source's tally, zero when it has none") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.resolutions(source) shouldBe 0
        d.bump(source, 3)
        d.resolutions(source) shouldBe 3
    }

    test("the Nth-time condition is an exact match on the tally") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val conditions = predicates.conditions
        val second = Conditions.SourceAbilityResolvedNTimes(2)
        d.bump(source, 1)
        conditions.evaluate(d.state, second, d.context(source)) shouldBe false
        d.bump(source, 2)
        conditions.evaluate(d.state, second, d.context(source)) shouldBe true
        d.bump(source, 3)
        conditions.evaluate(d.state, second, d.context(source)) shouldBe false
    }

    test("the cleanup step clears the tally") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.bump(source, 2)
        d.passPriorityUntil(Step.UPKEEP)
        d.resolutions(source) shouldBe 0
    }
})
