package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class NumericPropertyComparisonSerializationTest : FunSpec({
    test("every numeric property and operator round-trips with a composed amount") {
        for (property in CardNumericProperty.entries) for (operator in ComparisonOperator.entries) {
            val predicate: CardPredicate = CardPredicate.CompareNumericProperty(property, operator,
                DynamicAmount.Add(DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.Power),
                    DynamicAmount.Fixed(1)))
            val json = CardSerialization.json
            json.decodeFromString<CardPredicate>(json.encodeToString(predicate)) shouldBe predicate
        }
    }
    test("fluent target and object filters append the same predicate") {
        val amount = DynamicAmount.Fixed(2)
        val filter = GameObjectFilter.Creature.compareNumericProperty(CardNumericProperty.TOUGHNESS, ComparisonOperator.LT, amount)
        TargetFilter.Creature.compareNumericProperty(CardNumericProperty.TOUGHNESS, ComparisonOperator.LT, amount)
            .baseFilter shouldBe filter
    }
})
