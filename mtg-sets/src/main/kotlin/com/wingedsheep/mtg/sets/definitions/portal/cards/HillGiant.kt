package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hill Giant
 * {3}{R}
 * Creature — Giant
 * 3/3
 */
val HillGiant = card("Hill Giant") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Giant"
    power = 3
    toughness = 3

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Dan Frazier"
        flavorText = "Hill giants are mostly just big. Of course, that does count for a lot!"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3d4e5f6-a7b8-c9d0-e1f2-a3b4c5d6e7f8.jpg"
    }
}
