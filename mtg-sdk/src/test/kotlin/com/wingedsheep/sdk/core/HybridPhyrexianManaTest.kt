package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Hybrid Phyrexian mana — {R/G/P}, {R/W/P}, etc. (CR 107.4f): payable with one mana of either
 * color or 2 life, mana value 1, and both of its component colors.
 */
class HybridPhyrexianManaTest : StringSpec({

    "parses a hybrid Phyrexian pip into HybridPhyrexian" {
        ManaCost.parse("{R/G/P}").symbols shouldContainExactly listOf(
            ManaSymbol.HybridPhyrexian(Color.RED, Color.GREEN)
        )
    }

    "does not confuse a hybrid Phyrexian pip with a hybrid or Phyrexian pip" {
        ManaCost.parse("{R/G}").symbols.single() shouldBe ManaSymbol.Hybrid(Color.RED, Color.GREEN)
        ManaCost.parse("{R/P}").symbols.single() shouldBe ManaSymbol.Phyrexian(Color.RED)
    }

    "parses Lukka, Bound to Ruin's cost, round-trips, and has mana value 5" {
        val cost = ManaCost.parse("{2}{R}{R/G/P}{G}")
        cost.cmc shouldBe 5
        cost.toString() shouldBe "{2}{R}{R/G/P}{G}"
        ManaCost.parse(cost.toString()) shouldBe cost
    }

    "is both of its component colors" {
        ManaCost.parse("{R/W/P}").colors shouldBe setOf(Color.RED, Color.WHITE)
        ManaCost.parse("{R/W/P}").coloredSymbolCount(setOf(Color.RED, Color.WHITE)) shouldBe 1
    }

    "is a life-payable pip named by its first color" {
        val pip = ManaSymbol.HybridPhyrexian(Color.RED, Color.GREEN)
        pip.phyrexianLifeColor shouldBe Color.RED
        ManaCost.parse("{2}{R/G/P}").phyrexianSymbols shouldContainExactly listOf(pip)
        ManaCost.parse("{2}{R/G/P}").withPhyrexianPaidByLife(listOf(Color.RED)) shouldBe ManaCost.parse("{2}")
        ManaCost.parse("{2}{R/G/P}").withPhyrexianPaidByLife(listOf(Color.GREEN)) shouldBe null
    }

    "a life payment takes the single-colored pip before the hybrid one" {
        ManaCost.parse("{R/G/P}{R/P}").withPhyrexianPaidByLife(listOf(Color.RED)) shouldBe
            ManaCost.parse("{R/G/P}")
        ManaCost.parse("{R/G/P}{R/P}").withPhyrexianPaidByLife(listOf(Color.RED, Color.RED)) shouldBe
            ManaCost.ZERO
    }

    "relaxing colors treats it as one generic mana" {
        ManaCost.parse("{1}{R/G/P}").relaxColors() shouldBe ManaCost.parse("{2}")
    }
})
