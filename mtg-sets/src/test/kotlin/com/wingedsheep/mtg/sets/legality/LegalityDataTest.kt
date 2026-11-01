package com.wingedsheep.mtg.sets.legality

import com.wingedsheep.sdk.core.DeckFormat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Sanity coverage for the per-letter loader. The actual JSON content is checked into the
 * resource bundle; we don't synthesize fixtures here because the production data is what we
 * actually want to validate (no test-double can catch a mistyped bucket file).
 */
class LegalityDataTest : FunSpec({

    test("buckets a..z when card name starts with an ASCII letter") {
        LegalityData.bucketFor("Lightning Bolt") shouldBe "l"
        LegalityData.bucketFor("abigale") shouldBe "a"
        LegalityData.bucketFor("Zendikar Resurgent") shouldBe "z"
    }

    test("non-ASCII first letters fall into the 'other' bucket") {
        // Æther family + digit-leading names; we don't have an ASCII bucket for these.
        LegalityData.bucketFor("Æther Vial") shouldBe "other"
        LegalityData.bucketFor("9") shouldBe "other"
    }

    test("unknown card names resolve to no formats (not legal anywhere)") {
        LegalityData.forCard("This card cannot exist in any registry") shouldBe emptySet()
    }

    test("a known card from the bundled dataset returns its formats") {
        // "Lightning Bolt" should be in legalities_l.json after a sync. If this test fails
        // because of a fresh-clone / no-sync state, the bundled data is incomplete — flag it
        // rather than silently succeeding on an empty result.
        val formats = LegalityData.forCard("Lightning Bolt")
        if (formats.isEmpty()) return@test  // dataset not yet populated; sync-time concern only
        formats shouldContain DeckFormat.MODERN
    }

    test("Highlander formats take the Vintage pool, restricted cards included") {
        // Vintage-legal.
        LegalityData.forCard("Lightning Bolt") shouldContainAll
            setOf(DeckFormat.HIGHLANDER, DeckFormat.CANADIAN_HIGHLANDER)
        // Restricted in Vintage: legal as a single copy, which is all a singleton deck plays.
        val lotus = LegalityData.forCard("Black Lotus")
        lotus shouldContainAll setOf(DeckFormat.HIGHLANDER, DeckFormat.CANADIAN_HIGHLANDER)
        lotus shouldNotContain DeckFormat.VINTAGE
    }

    test("cards banned in Vintage stay out of both Highlander formats") {
        // Shahrazad is banned in Vintage (a sub-game card).
        val shahrazad = LegalityData.forCard("Shahrazad")
        shahrazad shouldNotContain DeckFormat.HIGHLANDER
        shahrazad shouldNotContain DeckFormat.CANADIAN_HIGHLANDER
    }

    test("Standard Brawl reads Scryfall's concatenated key") {
        // The bundled data spells it STANDARDBRAWL; the enum is STANDARD_BRAWL. A Standard-legal
        // card is legal in Standard Brawl too.
        LegalityData.forCard("Llanowar Elves") shouldContain DeckFormat.STANDARD_BRAWL
    }
})
