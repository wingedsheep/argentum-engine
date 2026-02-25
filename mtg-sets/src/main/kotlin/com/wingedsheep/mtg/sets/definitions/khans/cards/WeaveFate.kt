package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Weave Fate
 * {3}{U}
 * Instant
 * Draw two cards.
 */
val WeaveFate = card("Weave Fate") {
    manaCost = "{3}{U}"
    typeLine = "Instant"
    oracleText = "Draw two cards."

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Zack Stella"
        flavorText = "Temur shamans speak of three destinies: the now, the echo of the past, and the unwritten. They find flickering paths among tangled possibilities."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4a1f9c1-25a3-465f-b3d8-98f0867c9bb6.jpg?1562791439"
    }
}
