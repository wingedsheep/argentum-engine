package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.J
import com.wingedsheep.tooling.coverage.Link
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.dot
import com.wingedsheep.tooling.coverage.render
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonElement

/**
 * Pins the shared filter-predicate recovery the two filter renderers ([creatureFilterExpr] /
 * [gameObjectFilterExpr]) compose, so the IR→DSL fragments stay in one place. The predicates now return
 * the fluent chain [Link] they recover (read through the typed [com.wingedsheep.tooling.coverage] IrQuery
 * accessors, the power bound scoped to its `PowerIs` node), rendered here onto an empty base to assert
 * the historical `.suffix()` text.
 */
class FilterPredicatesTest : StringSpec({

    fun node(json: String): JsonElement = J.parseToJsonElement(json)

    /** The fluent suffix a recovered [Link] renders to (`.tapped()`), or null when absent. */
    fun suffix(link: Link?): String? = link?.let { render(Lit("").dot(it)) }

    // mtgish encodes a power bound as `PowerIs { _Comparison, args: Integer }` (Fleet-Footed Monk's
    // "creatures with power 2 or greater").
    fun powerIs(comparison: String, n: Int): String =
        """{"_Permanents":"PowerIs","args":{"_Comparison":"$comparison","args":{"_GameNumber":"Integer","args":$n}}}"""

    "power bounds recover from the scoped PowerIs node" {
        val atLeast = node(powerIs("GreaterThanOrEqualTo", 3))
        suffix(FilterPredicates.powerAtLeast(atLeast)) shouldBe ".powerAtLeast(3)"
        FilterPredicates.powerAtMost(atLeast).shouldBeNull()

        val atMost = node(powerIs("LessThanOrEqualTo", 2))
        suffix(FilterPredicates.powerAtMost(atMost)) shouldBe ".powerAtMost(2)"
        FilterPredicates.powerAtLeast(atMost).shouldBeNull()
    }

    "a power range keeps its two bounds distinct (scoped per PowerIs node)" {
        // power >= 2 AND power <= 4: each bound must read its OWN PowerIs clause, not the nearest integer.
        val range = node("""[${powerIs("GreaterThanOrEqualTo", 2)},${powerIs("LessThanOrEqualTo", 4)}]""")
        suffix(FilterPredicates.powerAtLeast(range)) shouldBe ".powerAtLeast(2)"
        suffix(FilterPredicates.powerAtMost(range)) shouldBe ".powerAtMost(4)"
    }

    "tap / attack state predicates map to their fluent suffixes" {
        suffix(FilterPredicates.tapped(node("""{"_Permanents":"IsTapped"}"""))) shouldBe ".tapped()"
        suffix(FilterPredicates.untapped(node("""{"_Permanents":"IsUntapped"}"""))) shouldBe ".untapped()"
        suffix(FilterPredicates.attacking(node("""{"_Permanents":"IsAttacking"}"""))) shouldBe ".attacking()"
        FilterPredicates.tapped(node("{}")).shouldBeNull()
        // IsUntapped must not be mistaken for IsTapped (substring containment would have).
        FilterPredicates.tapped(node("""{"_Permanents":"IsUntapped"}""")).shouldBeNull()
    }

    // mtgish encodes a toughness bound identically to a power bound but under the ToughnessIs tag.
    fun toughnessIs(comparison: String, n: Int): String =
        """{"_Permanents":"ToughnessIs","args":{"_Comparison":"$comparison","args":{"_GameNumber":"Integer","args":$n}}}"""

    "nontoken recovers from the IsNonToken marker" {
        suffix(FilterPredicates.nontoken(node("""{"_Permanents":"IsNonToken"}"""))) shouldBe ".nontoken()"
        FilterPredicates.nontoken(node("{}")).shouldBeNull()
    }

    "power-or-toughness recovers from the Or of equal >= bounds, declining a mismatch or a lone bound" {
        val both = node("""{"_Permanents":"Or","args":[${powerIs("GreaterThanOrEqualTo", 4)},${toughnessIs("GreaterThanOrEqualTo", 4)}]}""")
        suffix(FilterPredicates.powerOrToughnessAtLeast(both)) shouldBe ".powerOrToughnessAtLeast(4)"
        // unequal bounds must NOT widen "power or toughness" to one wrong number.
        val unequal = node("""{"_Permanents":"Or","args":[${powerIs("GreaterThanOrEqualTo", 4)},${toughnessIs("GreaterThanOrEqualTo", 2)}]}""")
        FilterPredicates.powerOrToughnessAtLeast(unequal).shouldBeNull()
        // a standalone power bound is not a power-or-toughness clause.
        FilterPredicates.powerOrToughnessAtLeast(node(powerIs("GreaterThanOrEqualTo", 4))).shouldBeNull()
    }

    "flying recovers as with/without keyword, distinguished by the DoesntHaveAbility marker" {
        val without = node("""{"_Permanents":"DoesntHaveAbility","args":[{"_Keyword":"Flying"}]}""")
        suffix(FilterPredicates.withoutFlying(without)) shouldBe ".withoutKeyword(Keyword.FLYING)"

        val plain = node("""{"_Permanents":"HasAbility","args":[{"_Keyword":"Flying"}]}""")
        FilterPredicates.withoutFlying(plain).shouldBeNull()
        suffix(FilterPredicates.withFlying(plain)) shouldBe ".withKeyword(Keyword.FLYING)"
    }
})
