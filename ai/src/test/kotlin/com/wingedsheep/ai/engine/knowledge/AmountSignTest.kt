package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AmountSignTest : FunSpec({
    val count = DynamicAmount.Count(Player.You, Zone.GRAVEYARD)

    test("fixed amounts read by their value") {
        AmountSign.of(DynamicAmount.Fixed(3)) shouldBe AmountSign.GAIN
        AmountSign.of(DynamicAmount.Fixed(-2)) shouldBe AmountSign.LOSS
        AmountSign.of(DynamicAmount.Fixed(0)) shouldBe AmountSign.ZERO
    }

    test("a count is a gain, and its negation a loss") {
        AmountSign.of(count) shouldBe AmountSign.GAIN
        AmountSign.of(DynamicAmount.Multiply(count, -1)) shouldBe AmountSign.LOSS
        AmountSign.of(DynamicAmount.Subtract(DynamicAmount.Fixed(0), count)) shouldBe AmountSign.LOSS
    }

    test("a difference of two counts could go either way") {
        AmountSign.of(DynamicAmount.Subtract(count, count)) shouldBe AmountSign.UNKNOWN
        AmountSign.of(DynamicAmount.IfPositive(DynamicAmount.Subtract(count, count))) shouldBe AmountSign.GAIN
    }

    test("a life total or a pipeline variable is not assumed non-negative") {
        AmountSign.of(DynamicAmount.YourLifeTotal) shouldBe AmountSign.UNKNOWN
        AmountSign.of(DynamicAmount.VariableReference("x")) shouldBe AmountSign.UNKNOWN
    }

    test("a modifier pair is a gain only when neither side can lower a stat") {
        statsSign(count, count) shouldBe AmountSign.GAIN
        statsSign(count, DynamicAmount.Fixed(0)) shouldBe AmountSign.GAIN
        statsSign(DynamicAmount.Fixed(2), DynamicAmount.Fixed(-2)) shouldBe AmountSign.UNKNOWN
        statsSign(DynamicAmount.Multiply(count, -1), DynamicAmount.Multiply(count, -1)) shouldBe AmountSign.LOSS
    }
})
