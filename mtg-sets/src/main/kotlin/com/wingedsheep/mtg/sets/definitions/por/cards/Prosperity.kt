package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Prosperity
 * {X}{U}
 * Sorcery
 * Each player draws X cards.
 */
val Prosperity = card("Prosperity") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"

    spell {
        effect = HandPatterns.eachPlayerDrawsX(
            includeController = true,
            includeOpponents = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/269bb4fc-9d8f-42cc-8f71-6a658e41533c.jpg"
    }
}
