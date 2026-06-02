package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Consider
 * {U}
 * Instant
 * Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 * Draw a card.
 */
val Consider = card("Consider") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\nDraw a card."

    spell {
        effect = Effects.Composite(
            LibraryPatterns.surveil(1),
            Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Zezhou Chen"
        flavorText = "Ivold gasped in surprise. Either a very strange insect had crawled onto one of the lenses or he was seeing geists at last!"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a211d505-4d40-4914-a9da-220770d6ddbc.jpg?1665819309"
    }
}
