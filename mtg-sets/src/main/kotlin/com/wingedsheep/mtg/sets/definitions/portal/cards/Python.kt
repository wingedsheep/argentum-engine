package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Python
 * {1}{B}{B}
 * Creature — Snake
 * 3/2
 */
val Python = card("Python") {
    manaCost = "{1}{B}{B}"
    typeLine = "Creature — Snake"
    power = 3
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Una Fricker"
        flavorText = "Silent and deadly, it strikes without warning."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82c552a1-6245-4caf-8249-765ce7ea80d2.jpg"
    }
}
