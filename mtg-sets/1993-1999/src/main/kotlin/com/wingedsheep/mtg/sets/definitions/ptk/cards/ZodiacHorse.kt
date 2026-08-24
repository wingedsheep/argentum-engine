package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Horse
 * {3}{G}
 * Creature — Horse
 * 3/3
 * Islandwalk (This creature can't be blocked as long as defending player controls an Island.)
 */
val ZodiacHorse = card("Zodiac Horse") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Horse"
    power = 3
    toughness = 3
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls an Island.)"

    keywords(Keyword.ISLANDWALK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Ai Desheng"
        flavorText = "\". . . 'First take Jingzhou, next the Riverlands; / On that rich region, base your own royal stand.' . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb24cb19-3409-45f7-b0e0-7c652064d2dd.jpg"
    }
}
