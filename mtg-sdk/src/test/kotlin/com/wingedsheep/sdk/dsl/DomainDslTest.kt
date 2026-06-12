package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for the Domain DSL surface — DynamicAmounts.domain() and
 * Conditions.BasicLandTypesAtLeast(n).
 */
class DomainDslTest : DescribeSpec({

    describe("DynamicAmounts.domain") {
        it("builds an AggregateBattlefield over lands you control with the basic-subtype aggregation") {
            val amount = DynamicAmounts.domain()
            amount.shouldBeInstanceOf<DynamicAmount.AggregateBattlefield>()
            amount.player shouldBe Player.You
            amount.filter shouldBe GameObjectFilter.Land
            amount.aggregation shouldBe Aggregation.DISTINCT_BASIC_LAND_SUBTYPES
        }

        it("renders Oracle-style description") {
            DynamicAmounts.domain().description shouldBe
                "the number of basic land types among lands you control"
        }

        it("supports counting domain for an opponent") {
            val amount = DynamicAmounts.domain(Player.AnOpponent)
            amount.shouldBeInstanceOf<DynamicAmount.AggregateBattlefield>()
            amount.player shouldBe Player.AnOpponent
            amount.description shouldBe
                "the number of basic land types among lands an opponent controls"
        }
    }

    describe("Conditions.BasicLandTypesAtLeast") {
        it("compares domain against a fixed threshold with GTE") {
            val condition = Conditions.BasicLandTypesAtLeast(3)
            condition.shouldBeInstanceOf<Compare>()
            condition.left shouldBe DynamicAmounts.domain()
            condition.operator shouldBe ComparisonOperator.GTE
            condition.right shouldBe DynamicAmount.Fixed(3)
        }
    }
})
