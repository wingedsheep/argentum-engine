package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scattered Thoughts
 * {3}{U}
 * Instant
 * Look at the top four cards of your library. Put two of those cards into your hand and the rest
 * into your graveyard.
 */
val ScatteredThoughts = card("Scattered Thoughts") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top four cards of your library. Put two of those cards into your hand and the rest into your graveyard."
    spell {
        effect = Patterns.Library.lookAtTopAndKeep(count = 4, keepCount = 2)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Brian Valeza"
        flavorText = "Stitchers delegate the most important tasks to assistants with a good eye for detail."
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc5c6675-6e0f-427d-9399-a6e7fc6215f1.jpg?1782703139"
    }
}
