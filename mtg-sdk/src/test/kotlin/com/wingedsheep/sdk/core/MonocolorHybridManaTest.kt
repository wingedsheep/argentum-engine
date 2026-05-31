package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Monocolored hybrid ("twobrid") mana — {2/B}, {2/G}, etc. Each pip is payable with the
 * generic amount OR one mana of the color, and has mana value equal to the generic component.
 */
class MonocolorHybridManaTest : StringSpec({

    "parses a monocolored hybrid pip into MonocolorHybrid" {
        ManaCost.parse("{2/B}").symbols shouldContainExactly listOf(
            ManaSymbol.MonocolorHybrid(2, Color.BLACK)
        )
    }

    "parses Gurmag Nightwatch's full cost" {
        ManaCost.parse("{2/B}{2/G}{2/U}").symbols shouldContainExactly listOf(
            ManaSymbol.MonocolorHybrid(2, Color.BLACK),
            ManaSymbol.MonocolorHybrid(2, Color.GREEN),
            ManaSymbol.MonocolorHybrid(2, Color.BLUE),
        )
    }

    "does not confuse a monocolored hybrid with a two-color hybrid or phyrexian pip" {
        ManaCost.parse("{2/W}").symbols.single() shouldBe ManaSymbol.MonocolorHybrid(2, Color.WHITE)
        ManaCost.parse("{W/U}").symbols.single() shouldBe ManaSymbol.Hybrid(Color.WHITE, Color.BLUE)
        ManaCost.parse("{W/P}").symbols.single() shouldBe ManaSymbol.Phyrexian(Color.WHITE)
    }

    "mana value of a monocolored hybrid is its generic component" {
        ManaSymbol.MonocolorHybrid(2, Color.RED).cmc shouldBe 2
        ManaCost.parse("{2/B}{2/G}{2/U}").cmc shouldBe 6
    }

    "round-trips through toString and parse" {
        val cost = ManaCost.parse("{2/B}{2/G}{2/U}")
        cost.toString() shouldBe "{2/B}{2/G}{2/U}"
        ManaCost.parse(cost.toString()) shouldBe cost
    }

    "reports the colored side in the cost's colors" {
        ManaCost.parse("{2/B}{2/G}{2/U}").colors shouldBe setOf(Color.BLACK, Color.GREEN, Color.BLUE)
    }

    "relaxing colors treats the colored side as one generic mana" {
        // {B} side becomes 1 mana of any type, so the whole cost relaxes to {3}.
        ManaCost.parse("{2/B}{2/G}{2/U}").relaxColors() shouldBe ManaCost.parse("{3}")
    }
})
