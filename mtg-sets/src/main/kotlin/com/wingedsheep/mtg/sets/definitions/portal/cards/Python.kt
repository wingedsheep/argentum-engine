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
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2c3d4e5-6f7a-8b9c-0d1e-2f3a4b5c6d7e.jpg"
    }
}
