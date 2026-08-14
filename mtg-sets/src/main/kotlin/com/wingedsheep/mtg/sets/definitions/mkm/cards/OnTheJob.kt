package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * On the Job — Murders at Karlov Manor #30
 * {2}{W}{W} · Instant
 *
 * Creatures you control get +2/+1 until end of turn. Investigate.
 *
 * Untargeted, unlike [AuspiciousArrival] — the Clue is never at risk of the spell being
 * countered on resolution for want of a legal target. The pump snapshots the battlefield as
 * the spell resolves (CR 611.2c): creatures that enter afterwards get nothing.
 */
val OnTheJob = card("On the Job") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +2/+1 until end of turn. Investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        effect = Effects.Composite(
            Patterns.Group.modifyStatsForAll(2, 1, Filters.Group.creaturesYouControl),
            Effects.Investigate()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Jason A. Engle"
        flavorText = "For all their differences, Kaya, Kellan, and Agrus Kos agreed on one thing: " +
            "none of them would rest until Teysa's murderer was brought to justice."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48b92629-4196-4943-91fd-8c8d5f3fcaef.jpg?1783912920"
    }
}
