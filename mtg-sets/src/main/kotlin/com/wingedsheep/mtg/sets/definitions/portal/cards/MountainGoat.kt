package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mountain Goat
 * {R}
 * Creature — Goat
 * 1/1
 * Mountainwalk
 */
val MountainGoat = card("Mountain Goat") {
    manaCost = "{R}"
    typeLine = "Creature — Goat"
    power = 1
    toughness = 1

    keywords(Keyword.MOUNTAINWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Heather Hudson"
        flavorText = "The mountain is as much its domain as the sky is the eagle's."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bc1d2e3-f4a5-b6c7-d8e9-f0a1b2c3d4e5.jpg"
    }
}
