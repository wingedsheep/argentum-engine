package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Granite
 * {2}{R}
 * Creature — Wall
 * 0/7
 * Defender
 */
val WallOfGranite = card("Wall of Granite") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 7

    keywords(Keyword.DEFENDER)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "John Matson"
        flavorText = "Solid as the mountain from which it came."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29d4e5f6-a7b8-c9d0-e1f2-a3b4c5d6e7f8.jpg"
    }
}
