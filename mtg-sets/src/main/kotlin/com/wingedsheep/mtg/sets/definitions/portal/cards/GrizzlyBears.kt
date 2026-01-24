package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grizzly Bears
 * {1}{G}
 * Creature — Bear
 * 2/2
 */
val GrizzlyBears = card("Grizzly Bears") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Bear"
    power = 2
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Jeff A. Menges"
        flavorText = "Don't try to outrun one of Dominaria's grizzlies; it'll catch you, knock you down, and eat you."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07095b44-e26a-47d5-a8c6-2e45a066fbc8.jpg"
    }
}
