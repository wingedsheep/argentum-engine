package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllLandsOfTypeEffect

/**
 * Flashfires
 * {3}{R}
 * Sorcery
 * Destroy all Plains.
 */
val Flashfires = card("Flashfires") {
    manaCost = "{3}{R}"
    typeLine = "Sorcery"

    spell {
        effect = DestroyAllLandsOfTypeEffect("Plains")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Dameon Willich"
        flavorText = "The prairies burn bright and fast."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39a0b1c2-d3e4-f5a6-b7c8-d9e0f1a2b3c4.jpg"
    }
}
