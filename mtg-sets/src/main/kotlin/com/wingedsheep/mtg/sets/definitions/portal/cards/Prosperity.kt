package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Prosperity
 * {X}{U}
 * Sorcery
 * Each player draws X cards.
 */
val Prosperity = card("Prosperity") {
    manaCost = "{X}{U}"
    typeLine = "Sorcery"

    spell {
        effect = Effects.EachPlayerDrawsX(
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
