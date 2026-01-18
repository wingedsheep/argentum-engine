package com.wingedsheep.rulesengine.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ManaSymbolTest : FunSpec({

    test("colored mana symbols have cmc of 1") {
        ManaSymbol.W.cmc shouldBe 1
        ManaSymbol.U.cmc shouldBe 1
        ManaSymbol.B.cmc shouldBe 1
        ManaSymbol.R.cmc shouldBe 1
        ManaSymbol.G.cmc shouldBe 1
    }

    test("generic mana has cmc equal to amount") {
        ManaSymbol.generic(0).cmc shouldBe 0
        ManaSymbol.generic(1).cmc shouldBe 1
        ManaSymbol.generic(5).cmc shouldBe 5
        ManaSymbol.generic(10).cmc shouldBe 10
    }

    test("colorless mana has cmc of 1") {
        ManaSymbol.C.cmc shouldBe 1
    }

    test("X mana has cmc of 0") {
        ManaSymbol.X.cmc shouldBe 0
    }

    test("colored mana toString formats correctly") {
        ManaSymbol.W.toString() shouldBe "{W}"
        ManaSymbol.U.toString() shouldBe "{U}"
        ManaSymbol.B.toString() shouldBe "{B}"
        ManaSymbol.R.toString() shouldBe "{R}"
        ManaSymbol.G.toString() shouldBe "{G}"
    }

    test("generic mana toString formats correctly") {
        ManaSymbol.generic(1).toString() shouldBe "{1}"
        ManaSymbol.generic(5).toString() shouldBe "{5}"
    }

    test("special mana toString formats correctly") {
        ManaSymbol.C.toString() shouldBe "{C}"
        ManaSymbol.X.toString() shouldBe "{X}"
    }

    test("colored mana contains correct color") {
        (ManaSymbol.W as ManaSymbol.Colored).color shouldBe Color.WHITE
        (ManaSymbol.U as ManaSymbol.Colored).color shouldBe Color.BLUE
        (ManaSymbol.B as ManaSymbol.Colored).color shouldBe Color.BLACK
        (ManaSymbol.R as ManaSymbol.Colored).color shouldBe Color.RED
        (ManaSymbol.G as ManaSymbol.Colored).color shouldBe Color.GREEN
    }
})
