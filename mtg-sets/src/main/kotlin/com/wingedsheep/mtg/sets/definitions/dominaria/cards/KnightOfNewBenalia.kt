package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Knight of New Benalia
 * {1}{W}
 * Creature — Human Knight
 * 3/1
 */
val KnightOfNewBenalia = card("Knight of New Benalia") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Josu Hernaiz"
        flavorText = "The mage-smiths of New Benalia have perfected the art of blending fine steel and enchanted glass into weapons that are both beautiful and deadly."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88c50c4b-a2c8-4cdc-a171-aa3ff9ef107f.jpg?1562739093"
    }
}
