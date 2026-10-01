package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.ProjectedValues
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.matchers.shouldBe

class NumericPropertyComparisonTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Forest")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        val evaluator = PredicateEvaluator(cardRegistry)
        for (operator in ComparisonOperator.entries) {
            test("$operator compares a candidate to smaller equal and larger amounts") {
                val game = board()
                val bear = game.findPermanent("Grizzly Bears")!!
                for (rhs in listOf(1, 2, 3)) {
                    val want = when (operator) {
                        ComparisonOperator.LT -> 2 < rhs
                        ComparisonOperator.LTE -> 2 <= rhs
                        ComparisonOperator.EQ -> 2 == rhs
                        ComparisonOperator.NEQ -> 2 != rhs
                        ComparisonOperator.GT -> 2 > rhs
                        ComparisonOperator.GTE -> 2 >= rhs
                    }
                    val filter = GameObjectFilter.Creature.compareNumericProperty(CardNumericProperty.TOUGHNESS,
                        operator, DynamicAmount.Fixed(rhs))
                    evaluator.matches(game.state, game.state.projectedState, bear, filter,
                        PredicateContext(game.player1Id)) shouldBe want
                }
            }
        }
        test("both operands read the caller's intermediate projection") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val projection = ProjectedState(game.state, mapOf(
                bear to ProjectedValues(power = 1, toughness = 5, types = setOf("CREATURE")),
                giant to ProjectedValues(power = 6, toughness = 1, types = setOf("CREATURE")),
            ))
            val filter = GameObjectFilter.Any.compareNumericProperty(CardNumericProperty.TOUGHNESS, ComparisonOperator.LT,
                DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.Power))
            evaluator.matches(game.state, projection, bear, filter, PredicateContext(game.player1Id, sourceId = giant)) shouldBe true
            val reverse = GameObjectFilter.Any.compareNumericProperty(CardNumericProperty.POWER, ComparisonOperator.GT,
                DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.Toughness))
            evaluator.matches(game.state, projection, bear, reverse, PredicateContext(game.player1Id, sourceId = giant)) shouldBe false
        }
        test("continuous-effect group filters evaluate numeric comparisons during projection") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val filter = com.wingedsheep.engine.mechanics.layers.AffectsFilter.Generic(
                com.wingedsheep.sdk.scripting.filters.unified.GroupFilter(
                    GameObjectFilter.Creature.compareNumericProperty(CardNumericProperty.TOUGHNESS,
                        ComparisonOperator.LT, DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.Power))))
            val intermediate = mapOf(
                bear to com.wingedsheep.engine.mechanics.layers.MutableProjectedValues(power = 2, toughness = 5,
                    types = mutableSetOf("CREATURE"), controllerId = game.player1Id),
                giant to com.wingedsheep.engine.mechanics.layers.MutableProjectedValues(power = 6, toughness = 6,
                    types = mutableSetOf("CREATURE"), controllerId = game.player1Id))
            val resolver = com.wingedsheep.engine.mechanics.layers.AffectsFilterResolver(evaluator)
            resolver.resolveAffectedEntities(game.state, giant, filter, intermediate) shouldBe setOf(bear)
        }
        test("missing context fails closed and a noncreature has no power or toughness") {
            val game = board()
            val filter = GameObjectFilter.Any.compareNumericProperty(CardNumericProperty.POWER,
                ComparisonOperator.EQ, DynamicAmount.Fixed(0))
            evaluator.matchesCardPredicate(game.state, game.state.projectedState,
                game.findPermanent("Grizzly Bears")!!, filter.cardPredicates.single(), null) shouldBe false
            evaluator.matches(game.state, game.state.projectedState, game.findPermanent("Forest")!!,
                filter, PredicateContext(game.player1Id)) shouldBe false
        }
        test("a creature made noncreature cannot match either P/T comparison") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            val projection = ProjectedState(game.state, mapOf(
                bear to ProjectedValues(power = 2, toughness = 2, types = setOf("LAND")),
            ))
            for (property in listOf(CardNumericProperty.POWER, CardNumericProperty.TOUGHNESS)) {
                val filter = GameObjectFilter.Any.compareNumericProperty(property,
                    ComparisonOperator.GT, DynamicAmount.Fixed(0))
                evaluator.matches(game.state, projection, bear, filter,
                    PredicateContext(game.player1Id)) shouldBe false
            }
        }
        test("a noncreature card outside the battlefield still has its printed P/T") {
            val game = board()
            val cardId = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(cardId) {
                val card = it.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!
                it.with(card.copy(typeLine = com.wingedsheep.sdk.core.TypeLine.artifact()))
            }
            game.state = game.zones.moveToZone(game.state, cardId, com.wingedsheep.sdk.core.Zone.GRAVEYARD).state
            for (property in listOf(CardNumericProperty.POWER, CardNumericProperty.TOUGHNESS)) {
                val filter = GameObjectFilter.Any.compareNumericProperty(property,
                    ComparisonOperator.EQ, DynamicAmount.Fixed(2))
                evaluator.matches(game.state, game.state.projectedState, cardId, filter,
                    PredicateContext(game.player1Id)) shouldBe true
            }
        }
        test("mana value and counters compare with composed amounts") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bear) { it.with(CountersComponent(mapOf(
                CounterType.CHARGE to 2, CounterType.PLUS_ONE_PLUS_ONE to 1))) }
            for ((property, rhs) in listOf(CardNumericProperty.MANA_VALUE to 2, CardNumericProperty.COUNTERS to 3)) {
                val filter = GameObjectFilter.Any.compareNumericProperty(property, ComparisonOperator.EQ,
                    DynamicAmount.Add(DynamicAmount.Fixed(rhs - 1), DynamicAmount.Fixed(1)))
                evaluator.matches(game.state, game.state.projectedState, bear, filter, PredicateContext(game.player1Id)) shouldBe true
            }
        }
        test("cross-entity references can bind earlier targets and negative values stay signed") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val projection = ProjectedState(game.state, mapOf(bear to ProjectedValues(power = -2, toughness = 2, types = setOf("CREATURE")),
                giant to ProjectedValues(power = -1, toughness = 3, types = setOf("CREATURE"))))
            val filter = GameObjectFilter.Any.compareNumericProperty(CardNumericProperty.POWER, ComparisonOperator.LT,
                DynamicAmount.EntityProperty(EffectTarget.ContextTarget(0), EntityNumericProperty.Power))
            val context = PredicateContext(game.player1Id, targets = listOf(
                com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(giant)))
            evaluator.matches(game.state, projection, bear, filter, context) shouldBe true
        }
    }
}
