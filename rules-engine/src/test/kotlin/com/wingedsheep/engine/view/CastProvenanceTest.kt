package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The two renderings of a cast's provenance. Both read off the same (alternative cost, origin zone)
 * pair, so this pins that they stay consistent with each other and that an ordinary cast from hand
 * stays silent rather than adding noise to every log line.
 */
class CastProvenanceTest : FunSpec({

    test("a plain cast from hand has no provenance to report") {
        CastProvenance.logPhrase(null, Zone.HAND) shouldBe null
        CastProvenance.badgeLabel(null, Zone.HAND) shouldBe null
    }

    test("an unresolved origin zone with no alternative cost reports nothing") {
        CastProvenance.logPhrase(null, null) shouldBe null
        CastProvenance.badgeLabel(null, null) shouldBe null
    }

    test("a disturb cast names both the mechanic and the graveyard it came from") {
        CastProvenance.logPhrase(AlternativeCostType.DISTURB, Zone.GRAVEYARD) shouldBe
            "disturb, from graveyard"
        CastProvenance.badgeLabel(AlternativeCostType.DISTURB, Zone.GRAVEYARD) shouldBe
            "Disturb · Graveyard"
    }

    test("an alternative cost paid from hand names only the mechanic") {
        CastProvenance.logPhrase(AlternativeCostType.EVOKE, Zone.HAND) shouldBe "evoke"
        CastProvenance.badgeLabel(AlternativeCostType.EVOKE, Zone.HAND) shouldBe "Evoke"
    }

    test("a normal cast from a zone other than hand names the zone alone") {
        CastProvenance.logPhrase(null, Zone.COMMAND) shouldBe "from command zone"
        CastProvenance.badgeLabel(null, Zone.COMMAND) shouldBe "Command zone"
        CastProvenance.logPhrase(null, Zone.EXILE) shouldBe "from exile"
    }

    test("an emerge cast names the body it ate and the mana it actually cost") {
        // The reported confusion: emerge {5}{U} minus a mana-value-2 sacrifice is a four-mana spell,
        // which reads as a bug unless both halves of that arithmetic are visible after the fact.
        CastProvenance.logPhrase(
            AlternativeCostType.EMERGE,
            Zone.HAND,
            sacrificedNames = listOf("Niblis of the Urn"),
            manaSpent = 4,
        ) shouldBe "emerge, sacrificed Niblis of the Urn, paid 4 mana"

        CastProvenance.sacrificeLabel(listOf("Niblis of the Urn")) shouldBe "Sacrificed Niblis of the Urn"
        CastProvenance.sacrificeLabel(emptyList()) shouldBe null
    }

    test("the sacrifice and the mana spent extend a phrase but never create one") {
        // A plain hand cast that sacrificed something for an *additional* cost (Angelic Purge) keeps
        // its silent log line: its printed cost is on the card and the sacrifice has its own event.
        CastProvenance.logPhrase(
            null,
            Zone.HAND,
            sacrificedNames = listOf("Thraben Inspector"),
            manaSpent = 3,
        ) shouldBe null
    }

    test("a graveyard cast reports the origin before the mana, and free casts stay quiet about it") {
        CastProvenance.logPhrase(AlternativeCostType.FLASHBACK, Zone.GRAVEYARD, manaSpent = 3) shouldBe
            "flashback, from graveyard, paid 3 mana"
        CastProvenance.logPhrase(AlternativeCostType.EVOKE, Zone.HAND, manaSpent = 0) shouldBe "evoke"
    }

    test("the mana actually paid renders as pips in WUBRG-then-colorless order") {
        CastProvenance.paidManaCost(white = 3, blue = 1, black = 0, red = 0, green = 0, colorless = 0) shouldBe
            "{W}{W}{W}{U}"
        CastProvenance.paidManaCost(white = 0, blue = 0, black = 1, red = 1, green = 1, colorless = 2) shouldBe
            "{B}{R}{G}{C}{C}"
        // Nothing spent — a free cast has no pips to show, so the badge stays off.
        CastProvenance.paidManaCost(0, 0, 0, 0, 0, 0) shouldBe null
    }

    test("every alternative cost has a player-facing name") {
        // Guards the exhaustive `when` against a new mechanic slipping through as a blank badge.
        AlternativeCostType.entries.forEach { type ->
            val phrase = CastProvenance.logPhrase(type, Zone.HAND)
            io.kotest.assertions.withClue("no name for $type") {
                (phrase != null && phrase.isNotBlank()) shouldBe true
            }
        }
    }
})
