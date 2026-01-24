package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllCreaturesWithColorEffect

/**
 * Nature's Ruin
 * {2}{B}
 * Sorcery
 * Destroy all green creatures.
 */
val NaturesRuin = card("Nature's Ruin") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        effect = DestroyAllCreaturesWithColorEffect(Color.GREEN)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Jeff Miracola"
        flavorText = "The forest withers and dies as darkness spreads."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f3c7d8f-4a07-4b3c-8c4a-6b9e8c2f5a1d.jpg"
    }
}
