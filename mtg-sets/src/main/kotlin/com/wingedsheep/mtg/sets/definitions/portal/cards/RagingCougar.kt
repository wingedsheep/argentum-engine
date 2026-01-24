package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Raging Cougar
 * {2}{R}
 * Creature — Cat
 * 2/2
 * Haste
 */
val RagingCougar = card("Raging Cougar") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 2

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Una Fricker"
        flavorText = "Swift as thought, deadly as action."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ef4a5b6-c7d8-e9f0-a1b2-c3d4e5f6a7b8.jpg"
    }
}
