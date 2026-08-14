package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Auspicious Arrival — Murders at Karlov Manor #5
 * {1}{W} · Instant
 *
 * Target creature gets +2/+2 until end of turn. Investigate.
 *
 * Single-target spell, so the Clue rides on the whole spell resolving: if the creature is an
 * illegal target by resolution the spell is countered on resolution (CR 608.2b) and nothing
 * happens at all — no pump *and* no Clue. That's the shape [ToxinAnalysis] already documents;
 * modelling investigate as a separate sentence in a [Effects.Composite] keeps that behaviour
 * because both halves live inside one spell resolution.
 */
val AuspiciousArrival = card("Auspicious Arrival") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. Investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.ModifyStats(2, 2, creature),
            Effects.Investigate()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Lie Setiawan"
        flavorText = "Proft brushed past the guards and introduced himself as \"the great Detective " +
            "Proft\" without a hint of irony, then identified the culprit within minutes."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/5180c85c-6add-4066-83c4-27fb1fd4de16.jpg?1783912929"
    }
}
