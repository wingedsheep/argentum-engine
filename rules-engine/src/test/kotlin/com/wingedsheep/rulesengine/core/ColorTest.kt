package com.wingedsheep.rulesengine.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull

class ColorTest : FunSpec({

    test("Color symbols are correct") {
        Color.WHITE.symbol shouldBe 'W'
        Color.BLUE.symbol shouldBe 'U'
        Color.BLACK.symbol shouldBe 'B'
        Color.RED.symbol shouldBe 'R'
        Color.GREEN.symbol shouldBe 'G'
    }

    test("fromSymbol returns correct color") {
        Color.fromSymbol('W') shouldBe Color.WHITE
        Color.fromSymbol('U') shouldBe Color.BLUE
        Color.fromSymbol('B') shouldBe Color.BLACK
        Color.fromSymbol('R') shouldBe Color.RED
        Color.fromSymbol('G') shouldBe Color.GREEN
    }

    test("fromSymbol returns null for invalid symbol") {
        Color.fromSymbol('X').shouldBeNull()
        Color.fromSymbol('1').shouldBeNull()
    }

    test("Color display names are correct") {
        Color.WHITE.displayName shouldBe "White"
        Color.BLUE.displayName shouldBe "Blue"
        Color.BLACK.displayName shouldBe "Black"
        Color.RED.displayName shouldBe "Red"
        Color.GREEN.displayName shouldBe "Green"
    }
})
