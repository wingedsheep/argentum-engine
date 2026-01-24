package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllLandsOfTypeEffect

/**
 * Boiling Seas
 * {3}{R}
 * Sorcery
 * Destroy all Islands.
 */
val BoilingSeas = card("Boiling Seas") {
    manaCost = "{3}{R}"
    typeLine = "Sorcery"

    spell {
        effect = DestroyAllLandsOfTypeEffect("Island")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "John Matson"
        flavorText = "The ocean itself turns to steam."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90a1b2c3-d0e1-f2a3-b4c5-d6e7f8a9b0c1.jpg"
    }
}
