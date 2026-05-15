package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Canopy Spider
 * {1}{G}
 * Creature — Spider
 * 1/3
 * Reach
 *
 * Note: original Tempest printing read "Canopy Spider can block creatures
 * with flying"; current Oracle uses the Reach keyword (functional reprint).
 */
val CanopySpider = card("Canopy Spider") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 1
    toughness = 3
    oracleText = "Reach"

    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "217"
        artist = "Christopher Rush"
        flavorText = "\"We know our place in the cycle of life—and it is not as flies.\"\n—Eladamri, Lord of Leaves"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afc114b0-2e95-4143-a4b6-6537813946e7.jpg?1562055929"
    }
}
