package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Rooster
 * {1}{G}
 * Creature — Bird
 * 2/1
 *
 * Plainswalk (This creature can't be blocked as long as defending player controls a Plains.)
 */
val ZodiacRooster = card("Zodiac Rooster") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 1
    oracleText = "Plainswalk (This creature can't be blocked as long as defending player controls a Plains.)"

    keywords(Keyword.PLAINSWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Ai Desheng"
        flavorText = "\". . . But the time of Han had run—could he [Kongming] not tell?— / That night his master star fell past the hills. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81dd362e-42f6-4eeb-9fe7-0d9cbeb4f21e.jpg"
    }
}
