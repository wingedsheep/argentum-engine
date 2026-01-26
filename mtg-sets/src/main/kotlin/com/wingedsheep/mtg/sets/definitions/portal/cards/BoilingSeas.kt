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
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1523c1b-2ba1-4581-8502-47544d450d8e.jpg"
    }
}
