package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Goat
 * {R}
 * Creature — Goat
 */
val ZodiacGoat = card("Zodiac Goat") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goat"
    power = 1
    toughness = 1
    oracleText = "Mountainwalk (This creature can't be blocked as long as defending player controls a Mountain.)"

    keywords(Keyword.MOUNTAINWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Qi Baocheng"
        flavorText = "\". . . Near death in Baidi, having reigned three years, / Bei sadly placed his son in Kongming's care. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52510462-2802-4e16-87d4-da376ee2e3be.jpg"
    }
}
