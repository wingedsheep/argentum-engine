package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.limited.SideboardDerivation
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [SideboardDerivation.fromPool] — the Limited sideboard = pool − maindeck rule
 * (CR 100.4b).
 */
class SideboardDerivationTest : FunSpec({

    fun card(name: String): CardDefinition =
        CardDefinition.sorcery(name, ManaCost.parse("{1}"), "")

    fun pool(vararg entries: Pair<String, Int>): List<CardDefinition> =
        entries.flatMap { (name, count) -> List(count) { card(name) } }

    test("leftover non-basic pool cards become the sideboard") {
        val pool = pool("Lightning Bolt" to 3, "Shock" to 2, "Counterspell" to 1)
        val deck = mapOf("Lightning Bolt" to 1, "Counterspell" to 1)

        SideboardDerivation.fromPool(pool, deck) shouldBe mapOf(
            "Lightning Bolt" to 2,  // 3 in pool − 1 played
            "Shock" to 2,           // 2 in pool − 0 played
        )
    }

    test("a card fully used in the deck does not appear in the sideboard") {
        val pool = pool("Shock" to 2)
        SideboardDerivation.fromPool(pool, mapOf("Shock" to 2)) shouldBe emptyMap()
    }

    test("basic lands are never sideboard material even if 'unused' in the pool") {
        val pool = pool("Mountain" to 10, "Shock" to 1)
        val deck = mapOf("Mountain" to 7, "Shock" to 1)

        // Mountains are excluded entirely; Shock is fully used → empty sideboard.
        SideboardDerivation.fromPool(pool, deck) shouldBe emptyMap()
    }

    test("playing more copies than the pool holds never yields a negative count") {
        val pool = pool("Shock" to 1)
        SideboardDerivation.fromPool(pool, mapOf("Shock" to 4)) shouldBe emptyMap()
    }

    test("an empty deck makes the whole non-basic pool the sideboard") {
        val pool = pool("Shock" to 2, "Forest" to 5)
        SideboardDerivation.fromPool(pool, emptyMap()) shouldBe mapOf("Shock" to 2)
    }
})
