package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Highland Giant
 * {2}{R}{R}
 * Creature — Giant
 * 3/4
 */
val HighlandGiant = card("Highland Giant") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Giant"
    power = 3
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Randy Gallegos"
        flavorText = "Though slow-witted and slow-moving, they are quick to anger."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2c3d4e5-f6a7-b8c9-d0e1-f2a3b4c5d6e7.jpg"
    }
}
