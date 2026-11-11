package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.limited.SideboardDerivation
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [SideboardDerivation.fromPool] — the Limited sideboard = pool − maindeck rule
 * (CR 100.4b), plus the free basic-land supply the Tournament Rules let a Limited player add.
 */
class SideboardDerivationTest : FunSpec({

    fun card(name: String): CardDefinition =
        CardDefinition.sorcery(name, ManaCost.parse("{1}"), "")

    fun pool(vararg entries: Pair<String, Int>): List<CardDefinition> =
        entries.flatMap { (name, count) -> List(count) { card(name) } }

    val supply = SideboardDerivation.basicLandSupply

    test("leftover non-basic pool cards become the sideboard") {
        val pool = pool("Lightning Bolt" to 3, "Shock" to 2, "Counterspell" to 1)
        val deck = mapOf("Lightning Bolt" to 1, "Counterspell" to 1)

        SideboardDerivation.fromPool(pool, deck) shouldBe mapOf(
            "Lightning Bolt" to 2,  // 3 in pool − 1 played
            "Shock" to 2,           // 2 in pool − 0 played
        ) + supply
    }

    test("a card fully used in the deck does not appear in the sideboard") {
        val pool = pool("Shock" to 2)
        SideboardDerivation.fromPool(pool, mapOf("Shock" to 2)) shouldBe supply
    }

    test("every sideboard carries the basic land supply, ten of each of the five basics") {
        val sideboard = SideboardDerivation.fromPool(pool("Shock" to 1), emptyMap())

        for (basic in listOf("Plains", "Island", "Swamp", "Mountain", "Forest")) {
            sideboard[basic] shouldBe SideboardDerivation.BASIC_LANDS_PER_TYPE
        }
    }

    test("pool basics fold into the supply, and playing basics never shrinks it") {
        val pool = pool("Mountain" to 10, "Shock" to 1)
        val deck = mapOf("Mountain" to 7, "Shock" to 1)

        // Basics are free: the Mountains the deck runs come from the same unlimited stock, so the
        // sideboard holds the full supply and nothing else (Shock is fully used).
        SideboardDerivation.fromPool(pool, deck) shouldBe supply
    }

    test("snow-covered basics and Wastes are pool cards, not part of the supply") {
        val pool = pool("Snow-Covered Forest" to 2, "Wastes" to 1)
        val deck = mapOf("Snow-Covered Forest" to 1)

        SideboardDerivation.fromPool(pool, deck) shouldBe
            mapOf("Snow-Covered Forest" to 1, "Wastes" to 1) + supply
    }

    test("playing more copies than the pool holds never yields a negative count") {
        val pool = pool("Shock" to 1)
        SideboardDerivation.fromPool(pool, mapOf("Shock" to 4)) shouldBe supply
    }

    test("an empty deck makes the whole non-basic pool the sideboard") {
        val pool = pool("Shock" to 2, "Forest" to 5)
        SideboardDerivation.fromPool(pool, emptyMap()) shouldBe mapOf("Shock" to 2) + supply
    }
})
