package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Call the Gatewatch
 * {2}{W}
 * Sorcery
 * Search your library for a planeswalker card, reveal it, put it into your hand, then shuffle.
 */
val CalltheGatewatch = card("Call the Gatewatch") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Search your library for a planeswalker card, reveal it, put it into your hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Planeswalker,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Yefim Kligerman"
        flavorText = "\"I've heard it said that a Planeswalker is someone who can always run from danger. But Gideon's right: we're also the ones who can choose to stay.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4719ac60-2cff-4abb-b9cc-b677d3ac945c.jpg"
    }
}
